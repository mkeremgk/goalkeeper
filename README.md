# Neural Network Goalkeeper

A Java simulation in which a small neural network predicts the lateral velocity needed for a goalkeeper to intercept a shot. The network, forward pass, backpropagation and gradient updates are implemented directly in Java, while Swing displays the ball and goalkeeper. Training uses analytically generated target velocities, so this is supervised learning rather than reinforcement learning.

## Results

The bundled model saved all 10 million shots in the original evaluation and in a separate seeded evaluation using the same simulated distribution. On the seeded shots, a stationary goalkeeper saved 27.27%. Normalized velocity RMSE was 0.0321 and final position RMSE was 0.575 simulation units. Three freshly trained networks also saved all shots in separate one million shot evaluations. The exact recorded values and source hashes are in [evaluation/recorded_metrics.json](evaluation/recorded_metrics.json), with console output in [evaluation/recorded_evaluation.txt](evaluation/recorded_evaluation.txt).

These results apply to straight shots from a fixed starting position, with randomized target and speed and a save tolerance of three units on either side of the keeper. There is no spin, bounce or measurement noise. Only the two velocity inputs vary in the generated starting states, and an analytic interception controller can solve the same task without learning. A perfect save rate therefore does not imply zero prediction error or performance in real football.

## Model

| Setting | Value |
|---|---|
| Architecture | 5 inputs, 8 hidden neurons, 1 output |
| Trainable parameters | 48 weights and 9 biases, totalling 57 |
| Inputs | Ball x/y position, ball x/y velocity and keeper x position |
| Output | Normalized keeper velocity |
| Activations | tanh in both layers |
| Training set | 10,000 generated examples |
| Default training | 100 epochs and 1,000,000 example updates |
| Learning rate | 0.001 |
| Optimizer | Gradient descent with an update after each example |

Each training example starts with a reset, after which the examples are shuffled and reused across epochs. Reset changes the simulated shot, not the learned weights. Training randomness is not seeded in the original implementation, while the evaluation command uses a fixed seed so that a particular saved model can be checked repeatedly. The bundled model's historical epoch count was not stored; the 100 epoch setting was checked separately with fresh training runs.

## Build and run

A JDK is the only dependency. The repository was tested with JDK 26 on macOS. Run commands from the repository root:

```sh
mkdir -p build
javac -d build AI.java Game.java Main.java Evaluation.java GoalkeeperTests.java
java -cp build Main play
```

The application loads `models/goalkeeper.ser`, opens the Swing window and starts a shot. Use **Shoot Again** for another shot. A graphical desktop is needed only for this command. Opening the application does not train or overwrite the model.

If `make` is installed, `make run` performs the same build and launch.

## Evaluate and test

```sh
java -Djava.awt.headless=true -cp build GoalkeeperTests
java -Djava.awt.headless=true -cp build Main evaluate 10000000 20261107
```

The evaluation prints one JSON record with save counts, the stationary comparison and regression errors. A shorter smoke evaluation can use `10000` shots. `make test` runs the regression checks and `make evaluate` runs the default ten million shot evaluation. The tests check architecture, normalization, repeatable evaluation, the bundled model and invalid arguments. They do not validate a real sports application.

## Train a separate model

```sh
java -Djava.awt.headless=true -cp build Main train 100 build/my-agent.ser
java -Djava.awt.headless=true -cp build Main evaluate 1000000 20261107 build/my-agent.ser
java -cp build Main play build/my-agent.ser
```

Training refuses to overwrite an existing output file; choose another filename for another run. The bundled model is kept separate from generated models. Java serialization is used for persistence, so load only the bundled model or a trusted model you produced with this code.

## Files and reproducibility

`AI.java` defines the network and trainer, `Game.java` provides the world and Swing animation, and `Main.java` exposes the play, train and evaluate commands. `Evaluation.java` implements seeded evaluation with the same shot distribution and continuous interception rule as the original test. `GoalkeeperTests.java` checks that the packaged application behaves as documented.

The 7 November 2026 measurements were made before packaging changes to the command line entry point. Network and simulation code and bundled model bytes are preserved. The original source hashes in the recorded metrics describe that earlier entry point; they should not be mistaken for hashes of the current `Main.java`. The packaged evaluation reproduces the saved model's seeded metrics.

The repository includes source, a 1.1 kB trained model and compact numerical evidence. Build products and newly trained models stay in the ignored `build` directory. No open source licence has been selected for the project code.
