import java.nio.file.Path;

/** No external test dependencies; fails with a nonzero exit code. */
public class GoalkeeperTests {
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        NeuralNetwork nn = Main.loadAgent(Path.of("models/goalkeeper.ser"));
        int parameters = 0;
        for (Layer layer : new Layer[]{nn.hiddenLayer, nn.outputLayer}) {
            for (Neuron neuron : layer.neurons) parameters += neuron.weights.length+1;
        }
        check(parameters == 57, "Model architecture changed");
        World world = new World();
        check(world.convertToMinusOneOne(8, 8, 32) == -1, "Lower normalization bound");
        check(world.convertToMinusOneOne(32, 8, 32) == 1, "Upper normalization bound");
        Evaluation.Result a = Evaluation.measure(nn, 10_000, 20261107L);
        Evaluation.Result b = Evaluation.measure(nn, 10_000, 20261107L);
        check(a.equals(b), "Seeded evaluation must repeat exactly");
        check(a.saves() == 10_000, "Bundled model regression on fixed shot set");
        check(a.stationarySaves() > 2400 && a.stationarySaves() < 3100, "Stationary baseline distribution");
        check(a.normalizedMse() > 0 && a.normalizedMse() < .002, "Velocity regression error");
        boolean rejected = false;
        try { Evaluation.measure(nn, 0, 1); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "Nonpositive shot counts must fail");
        System.out.println("Passed architecture, normalization, deterministic evaluation, model regression, baseline and argument checks.");
    }
}
