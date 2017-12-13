import java.io.Serializable;
import java.util.Arrays;
import java.io.Serializable;
/// <summary>
/// Each individual layer in the ML{
/// </summary>
public class Layer implements Serializable
{
	double unique;
	private static final long serialVersionUID = 1L;
	
	int numberOfInputs; //# of neurons in the previous layer
    int numberOfOuputs; //# of neurons in the current layer

    double[] outputs; //outputs of this layer
    double[] inputs; //inputs in into this layer
    double[][] weights; //weights of this layer
    double[][] weightsDelta; //deltas of this layer
    double[] gamma; //gamma of this layer
    double[] error; //error of the output layer

    double[] recurrentOutputs; 
    
    /// <summary>
    /// Constructor initilizes vaiour data structures
    /// </summary>
    /// <param name="numberOfInputs">Number of neurons in the previous layer</param>
    /// <param name="numberOfOuputs">Number of neurons in the current layer</param>
    
    public Layer(int numberOfInputs, int numberOfOuputs)
    {
    	
    	numberOfInputs++;
    		
    	recurrentOutputs = new double[numberOfOuputs];
    		
    	//make the network recurrent (memory)
    	//the output of the next layer, is (in the next cycle) added to the input of the privious player
    	numberOfInputs += numberOfOuputs;
   
    	
        this.numberOfInputs = numberOfInputs;
        this.numberOfOuputs = numberOfOuputs;

        //initilize datastructures
        outputs = new double[numberOfOuputs];
        inputs = new double[numberOfInputs];
        weights = new double[numberOfOuputs][numberOfInputs];
        weightsDelta = new double[numberOfOuputs][numberOfInputs];
        gamma = new double[numberOfOuputs];
        error = new double[numberOfOuputs];

        InitilizeWeights(); //initilize weights
    }
    
    //reset memory (recurrentOutputs)
    void resetLayer() {
    	recurrentOutputs = new double[numberOfOuputs];
    }

    /// <summary>
    /// Initilize weights between -0.5 and 0.5
    /// </summary>
    public void InitilizeWeights()
    {
        for (int i = 0; i < numberOfOuputs; i++)
        {
            for (int j = 0; j < numberOfInputs; j++)
            {
                weights[i][j] = (double) (Math.random() - 0.5f);
            }
        }
    }

    /// <summary>
    /// Feedforward this layer with a given input
    /// </summary>
    /// <param name="inputs">The output values of the previous layer</param>
    /// <returns></returns>
    public double[] FeedForward(double[] inputs)
    {
        inputs = addItemToArray(inputs, 1);
        inputs = addItemsToArray(inputs, recurrentOutputs);
        
        this.inputs = inputs;// keep shallow copy which can be used for back propagation

        //feed forwards
        for (int i = 0; i < numberOfOuputs; i++)
        {
            outputs[i] = 0;
            for (int j = 0; j < numberOfInputs; j++)
            {
                outputs[i] += inputs[j] * weights[i][j];
            }

            outputs[i] = (double) Math.tanh(outputs[i]);
        }
        
        recurrentOutputs = outputs.clone();

        return outputs;
    }
    
    public double[] addItemToArray(double[] array, double item){
    	double[] newArray = new double[array.length + 1];
    	for(int i = 0; i < array.length; i++){
    		newArray[i] = array[i];
    	}
    	newArray[newArray.length - 1] = item;

    	return newArray;
    }
    
    public double[] addItemsToArray(double[] array, double[] items){
    	double[] newArray = new double[array.length + items.length];
    	for(int i = 0; i < newArray.length; i++){
    		if(i < array.length)
    			newArray[i] = array[i];
    		else
    			newArray[i] = items[i - array.length];
    	}

    	return newArray;
    }
    

    /// <summary>
    /// TanH derivate 
    /// </summary>
    /// <param name="value">An already computed TanH value</param>
    /// <returns></returns>
    public double TanHDer(double value)
    {
        return 1 - (value * value);
    }
    
    public Layer copy(){
        Layer copyLayer = new Layer(numberOfInputs, numberOfOuputs);
        
        copyLayer.numberOfInputs = numberOfInputs;
        copyLayer.numberOfOuputs = numberOfOuputs;
        
        copyLayer.outputs = outputs.clone();
        copyLayer.inputs = inputs.clone();
        
        copyLayer.weights = new double[weights.length][];
        for(int i = 0; i < weights.length; i++){
            copyLayer.weights[i] = weights[i].clone();
        }
        
        copyLayer.weightsDelta = new double[weightsDelta.length][];
         for(int i = 0; i < weightsDelta.length; i++){
            copyLayer.weightsDelta[i] = weightsDelta[i].clone();
        }
         
        copyLayer.gamma = gamma.clone();
        copyLayer.error = error.clone();
        copyLayer.recurrentOutputs = recurrentOutputs.clone();
        
        return copyLayer;
    }

    /// <summary>
    /// Back propagation for the output layer
    /// </summary>
    /// <param name="expected">The expected output</param>
    public void BackPropOutput(double[] expected)
    {
        //Error dervative of the cost function
        for (int i = 0; i < numberOfOuputs; i++)
            error[i] = outputs[i] - expected[i];

        //Gamma calculation
        for (int i = 0; i < numberOfOuputs; i++)
            gamma[i] = error[i] * TanHDer(outputs[i]);

        //Caluclating detla weights
        for (int i = 0; i < numberOfOuputs; i++)
        {
            for (int j = 0; j < numberOfInputs; j++)
            {
                weightsDelta[i][j] = gamma[i] * inputs[j];
            }
        }
    }

    /// <summary>
    /// Back propagation for the hidden layers
    /// </summary>
    /// <param name="gammaForward">the gamma value of the forward layer</param>
    /// <param name="weightsFoward">the weights of the forward layer</param>
    public void BackPropHidden(double[] gammaForward, double[][] weightsFoward)
    {
        //Caluclate new gamma using gamma sums of the forward layer
        for (int i = 0; i < numberOfOuputs; i++)
        {
            gamma[i] = 0;

            for (int j = 0; j < gammaForward.length; j++)
            {
                gamma[i] += gammaForward[j] * weightsFoward[j][i];
            }

            gamma[i] *= TanHDer(outputs[i]);
        }

        //Caluclating detla weights
        for (int i = 0; i < numberOfOuputs; i++)
        {
            for (int j = 0; j < numberOfInputs; j++)
            {
                weightsDelta[i][j] = gamma[i] * inputs[j];
            }
        }
    }  

    /// <summary>
    /// Updating weights
    /// </summary>
    public void UpdateWeights()
    {
        for (int i = 0; i < numberOfOuputs; i++)
        {
            for (int j = 0; j < numberOfInputs; j++)
            {
                weights[i][j] -= weightsDelta[i][j] * Main.LEARNING_RATE;
            }
        }
    }
}
