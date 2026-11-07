import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;

// holds network inputs and expected outputs
class TrainingExample {
    double[] inputs;
    double[] targets;
}

//Creates randomized dataset for training( good data + as much as we want = good for supervised learning :) )
class TrainingExampleGenerator {
    World world = new World();

    final double MAX_KEEPER_VELOCITY = 10.0;

    //generates normalized inputs and targets ( nn likes working with samll nuumbers)
    public TrainingExample[] generate() {
        TrainingExample[] trainingExamples = new TrainingExample[10000];

        for (int i = 0; i < trainingExamples.length; i++) 
        {
            
                world.reset();

             double[] inputs = world.getInputs();

             double time = world.ball.position.y / world.ball.velocity.y;
             double targetX = world.ball.position.x + world.ball.velocity.x * time;

             double desiredKeeperVelocity = (targetX - world.keeper.position.x) / time;

             double normalizedVelocity = desiredKeeperVelocity / MAX_KEEPER_VELOCITY;

             if (normalizedVelocity > 1) normalizedVelocity = 1;
             if (normalizedVelocity < -1) normalizedVelocity = -1;

            trainingExamples[i] = new TrainingExample();
            trainingExamples[i].inputs = inputs;

            trainingExamples[i].targets = new double[]{normalizedVelocity};
        }

        return trainingExamples;
    }
}

// craete neural network training loops
class Trainer {
    NeuralNetwork nn;
    TrainingExample[] tes;
    int epochs;

    final double learning_rate = 0.001;

    public Trainer(NeuralNetwork neuralNetwork, TrainingExample[] trainingExamples, int epochs) {
        this.nn = neuralNetwork;
        this.tes = trainingExamples;
        this.epochs = epochs;
    }

    // Runs epochs/generations  and updates network weights
    public void train()
     {
        for (int epoch = 0; epoch < epochs; epoch++)
        {
            Collections.shuffle(Arrays.asList(tes));

            double epoch_error = 0;

            for (TrainingExample te : tes) {
                double[] outputs = nn.getOutputs(te.inputs);
                epoch_error += (te.targets[0] - outputs[0]) * (te.targets[0] - outputs[0]);

                nn.calculateOutputDeltas(outputs, te);
                nn.calculateHiddenDeltas();

                nn.calculateOutputWeights(learning_rate);
                nn.calculateOutputBias(learning_rate);

                nn.calculateHiddenWeights(learning_rate, te.inputs);
                nn.calculateHiddenBias(learning_rate);
            }

            epoch_error /= tes.length;
            if (epoch % 100 == 0) {
                System.out.println("epoch " + epoch + " error: " + epoch_error);
            }
        }
    }
}

//implements a multi-layer perceptron
class NeuralNetwork implements Serializable 
{

    private static final long serialVersionUID = 1L;

    Layer hiddenLayer;
    Layer outputLayer;

    // Initializes hidden and output layers ( 8 neurons hidden layer and draws from 5 neurons  for each elemnet in input layer)
    // outputlaYer has 1 nueron velocity of keeper and draws from the hidden layers 8 neurons
    public NeuralNetwork() {
        hiddenLayer = new Layer(createNeurons(8, 5), 5, 8);

        outputLayer = new Layer(createNeurons(1, 8), 8, 1);
    }
  
    //starts each weightt at a random small value ( the model needs something to latch onto at the beggining
    //  and for all weight nnot to predict the same)
    private Neuron[] createNeurons(int neuronCount, int weightCount) {
        Neuron[] neurons = new Neuron[neuronCount];

        for (int i = 0; i < neuronCount; i++) {
            double[] weights = new double[weightCount];

            for (int w = 0; w < weightCount; w++)
                 {
                weights[w] = Math.random() * 0.2 - 0.1;
            }
            neurons[i] = new Neuron(weights, 0);
        }

        return neurons;
    }

// Computes forward pass through network
    //
    public double[] getOutputs(double[] inputs) {
        double[] hiddenOutputs = new double[8];

        for (int i = 0; i < 8; i++) 
            {

            hiddenLayer.neurons[i].sumUp(inputs);
            hiddenOutputs[i] = hiddenLayer.neurons[i].output;
        }

        double[] outputs = new double[1];
        outputLayer.neurons[0].sumUp(hiddenOutputs);
        outputs[0] = outputLayer.neurons[0].output;

        return outputs;
    }

    // calculates error  for output
    public void calculateOutputDeltas(double[] outputs, TrainingExample te) {
        outputLayer.neurons[0].delta =(te.targets[0] - outputs[0]) * (1 - outputs[0] * outputs[0]);
    }

// same  for hidden layer
    public void calculateHiddenDeltas() {
        for (int i = 0; i < 8; i++) {
            double hiddenError =outputLayer.neurons[0].delta * outputLayer.neurons[0].weights[i];

            hiddenLayer.neurons[i].delta =  hiddenError * (1 - hiddenLayer.neurons[i].output * hiddenLayer.neurons[i].output);
        }
    }

    //updates weights for output layer
    public void calculateOutputWeights(double learningRate) {
        for (int i = 0; i < 8; i++) {
            double change = learningRate * outputLayer.neurons[0].delta * hiddenLayer.neurons[i].output;
            outputLayer.neurons[0].weights[i] += change;
        }
    }
    // Updates bias for output layer
    public void calculateOutputBias(double learningRate) {
        outputLayer.neurons[0].bias += learningRate * outputLayer.neurons[0].delta;
    }

    //same for hidden layer
    public void calculateHiddenWeights(double learningRate, double[] inputs) {
        for (int h = 0; h < 8; h++) {
            for (int i = 0; i < 5; i++) {
                double change =
                        learningRate * hiddenLayer.neurons[h].delta * inputs[i];

                hiddenLayer.neurons[h].weights[i] += change;
            }
        }
    }

// same for hidden layer
    public void calculateHiddenBias(double learningRate) 
    {
        for (Neuron neuron : hiddenLayer.neurons) {
            neuron.bias += learningRate * neuron.delta;
        }
    }
}

// groups neurons into a Layer
class Layer implements Serializable {
    private static final long serialVersionUID = 1L;

    Neuron[] neurons;
    int inputSize;
    int outputSize;

    //initializes it''s properties
    public Layer(Neuron[] neurons, int inputSize, int outputSize) {
        this.neurons = neurons;
        this.inputSize = inputSize;
        this.outputSize = outputSize;
    }
}
//
// Represents 1 neuron
class Neuron implements Serializable 
{
    private static final long serialVersionUID = 1L;

    double[] weights;
    double bias;
    double rawSum = 0;
    double output = 0;
    double delta;
    //initializes weights and bias
    public Neuron(double[] weights, double bias) {
        this.weights = weights;
        this.bias = bias;
    }
    //calculates weigted sum and activation
    public void sumUp(double[] inputs) 
    {
        rawSum = 0;

        for (int i = 0; i < inputs.length; i++) 
        {
            rawSum += inputs[i] * weights[i];
        }

        rawSum += bias;

        output = Math.tanh(rawSum);
    }
}