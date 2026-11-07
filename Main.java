import java.io.*;
import java.nio.file.*;
import javax.swing.SwingUtilities;

/** Explicit training and evaluation commands; opening the app loads the bundled model. */
public class Main {
    static final double MAX_KEEPER_VELOCITY = 10.0;

    public static void main(String[] args) throws Exception {
        String command = args.length == 0 ? "play" : args[0];
        if (command.equals("--help") || command.equals("help")) {
            System.out.println("java -cp build Main [play [model] | evaluate [shots] [seed] [model] | train [epochs] [output]]");
            System.out.println("Defaults: models/goalkeeper.ser; 10000000 shots; seed 20261107; 100 epochs.");
            System.out.println("Training writes build/trained-agent.ser and refuses to overwrite an existing file.");
            return;
        }
        switch (command) {
            case "play": {
                if (args.length > 2) throw new IllegalArgumentException("play accepts one optional model path");
                NeuralNetwork nn = loadAgent(Path.of(args.length > 1 ? args[1] : "models/goalkeeper.ser"));
                SwingUtilities.invokeLater(() -> {
                    World world = new World();
                    Screen screen = new Screen();
                    screen.setShootAgainAction(() -> shoot(world, nn, screen));
                    screen.showScreen();
                    shoot(world, nn, screen);
                });
                break;
            }
            case "evaluate": {
                if (args.length > 4) throw new IllegalArgumentException("evaluate accepts shots, seed and model path");
                int shots = args.length > 1 ? positive(args[1]) : 10_000_000;
                long seed = args.length > 2 ? Long.parseLong(args[2]) : 20261107L;
                Path model = Path.of(args.length > 3 ? args[3] : "models/goalkeeper.ser");
                System.out.println(Evaluation.measure(loadAgent(model), shots, seed).json());
                break;
            }
            case "train": {
                if (args.length > 3) throw new IllegalArgumentException("train accepts epochs and output path");
                int epochs = args.length > 1 ? positive(args[1]) : 100;
                Path output = Path.of(args.length > 2 ? args[2] : "build/trained-agent.ser");
                if (Files.exists(output)) throw new IOException("Refusing to overwrite " + output);
                NeuralNetwork nn = new NeuralNetwork();
                TrainingExample[] examples = new TrainingExampleGenerator().generate();
                new Trainer(nn, examples, epochs).train();
                if (output.toAbsolutePath().getParent() != null) Files.createDirectories(output.toAbsolutePath().getParent());
                try (ObjectOutputStream stream = new ObjectOutputStream(Files.newOutputStream(output, StandardOpenOption.CREATE_NEW))) {
                    stream.writeObject(nn);
                }
                System.out.println("Saved " + output + "; examples=" + examples.length + "; epochs=" + epochs);
                break;
            }
            default: throw new IllegalArgumentException("Unknown command: " + command + ". Use --help.");
        }
    }

    static int positive(String text) {
        int value = Integer.parseInt(text);
        if (value <= 0) throw new IllegalArgumentException("Expected a positive integer");
        return value;
    }

    static NeuralNetwork loadAgent(Path path) throws IOException, ClassNotFoundException {
        // Load only the bundled model or a trusted model produced by this project.
        try (ObjectInputStream stream = new ObjectInputStream(Files.newInputStream(path))) {
            return (NeuralNetwork) stream.readObject();
        }
    }

    static void shoot(World world, NeuralNetwork nn, Screen screen) {
        world.reset();
        double velocity = nn.getOutputs(world.getInputs())[0] * MAX_KEEPER_VELOCITY;
        screen.panel.startSimulation(world.ball.position.x, world.ball.position.y,
                world.ball.velocity.x / 10.0, world.ball.velocity.y / 10.0, velocity / 10.0);
    }
}
