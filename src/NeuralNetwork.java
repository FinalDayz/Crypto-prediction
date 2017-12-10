
public class NeuralNetwork
{

    int[] layer; //layer information
    Layer[] layers; //layers in the network

    /// <summary>
    /// Constructor setting up layers
    /// </summary>
    /// <param name="layer">Layers of this network</param>
    public NeuralNetwork(int[] layer)
    {
        //deep copy layers
        this.layer = new int[layer.length];
        for (int i = 0; i < layer.length; i++)
            this.layer[i] = layer[i];

        //creates neural layers
        layers = new Layer[layer.length-1];

        for (int i = 0; i < layers.length; i++)
        {
            layers[i] = new Layer(layer[i], layer[i+1]);
        }
    }
    
    //reset memory, if it done with one session
    void resetNetwork() {
    	for (int i = 0; i < layers.length; i++){
    		layers[i].resetLayer();
    	}
    }

    /// <summary>
    /// High level feedforward for this network
    /// </summary>
    /// <param name="inputs">Inputs to be feed forwared</param>
    /// <returns></returns>
    public double[] FeedForward(double[] inputs)
    {
        //feed forward
        layers[0].FeedForward(inputs);
        for (int i = 1; i < layers.length; i++)
        {
            layers[i].FeedForward(layers[i-1].outputs);
        }

        return layers[layers.length - 1].outputs; //return output of last layer
    }
    
    
    double[] getOutput(double[] inputs){
        //feed forward
        this.layers[0].FeedForward(inputs);

        for (int i = 1; i < this.layers.length; i++)
        {
            this.layers[i].FeedForward(this.layers[i-1].outputs);
        }
        double[] output = this.layers[this.layers.length - 1].outputs;
        for (int i = 0; i < output.length; i++)
        {
          output[i] = Math.round(output[i]*10)/10.0;
        }
        return  output;//return output of last layer
      }
    
    NeuralNetwork copy() {
        NeuralNetwork NN = new NeuralNetwork(layer.clone());
        NN.layer = layer.clone();
        
        for(int i = 0;i < layers.length; i++){
            NN.layers[i] = layers[i].copy();
        }
        
        return NN;
    }

    /// <summary>
    /// High level back porpagation
    /// Note: It is expexted the one feed forward was done before this back prop.
    /// </summary>
    /// <param name="expected">The expected output form the last feedforwar d</param>
    public double BackProp(double[] expected, boolean... getError)
    {	
    	double[] errorOutput = null;
        // run over all layers backwards
        for (int i = layers.length-1; i >=0; i--)
        {
            if(i == layers.length - 1)
            {
                layers[i].BackPropOutput(expected); //back prop output
                errorOutput = layers[i].error;
            }
            else
            {
                layers[i].BackPropHidden(layers[i+1].gamma, layers[i+1].weights); //back prop hidden
            }
        }

        //Update weights
        for (int i = 0; i < layers.length; i++)
        {
            layers[i].UpdateWeights();
        }
        
        if(getError.length > 0 && getError[0] == true){
            double AverageError = 0;
            for(int i = 0; i < errorOutput.length; i++){
              AverageError += Math.abs(errorOutput[i]);
            }
            AverageError /= errorOutput.length;
            return AverageError;
          }
        return 0;
    }
}
