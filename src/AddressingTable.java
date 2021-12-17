import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class AddressingTable implements Serializable {

    class Value implements Serializable{
        Integer nextNode;
        Integer cost ;
        Boolean isOn;
        public Value(Integer nextNode , Integer cost, Boolean isOn)  // key: nextNodeId    value: cost
        {
            this.nextNode= nextNode;
            this.cost=cost;
            this.isOn=isOn;
        }

        public Value(){
            this.nextNode=-1;
            this.cost=-1;
            this.isOn=true;
        }

        public int getCost(){
            return this.cost;
        }
        public int getNextNode(){
            return this.nextNode;
        }
        public boolean getIsOn(){
            return this.isOn;
        }


        public void setCost(Integer cost) {
            this.cost = cost;
        }

        @Override
        public String toString() {
            return "Value{" +
                    "nextNode=" + nextNode +
                    ", cost=" + cost +
                    ", isOn=" + isOn +
                    '}';
        }
    }


    private HashMap<Integer, ArrayList<Value>> distanceVector;  // key: destinyNodeId   value: pair of nextNodeId and cost


    public AddressingTable(Integer nodeId){
        this.distanceVector = new HashMap<>();
        this.setDistance(nodeId, nodeId, 0, true); // o custo para chegar a ele mesmo é 0
    }




    // ------------------------------ GETTERS AND SETTERS ------------------------------


    // creates or updates in distanceVector
    public void setDistance(Integer destinyNode, Integer nextNode, Integer cost, Boolean isOn){

        // se o destinyNode já existe na tabela, vai atualizar/adicionar à lista de values
        if(this.distanceVector.containsKey(destinyNode))
            setDistanceInValueList(this.distanceVector.get(destinyNode), nextNode, cost, isOn);

        // se o destinyNode não existe na tabela, vai criar uma nova entrada com uma lista de um elem
        else{
            ArrayList<Value> valuesList = new ArrayList<>();
            Value value = new Value(nextNode, cost, isOn);
            valuesList.add(value);
            this.distanceVector.put(destinyNode, valuesList);
        }
    }

    // creates or updates a Value in a ValueList
    public void setDistanceInValueList(ArrayList<Value> valueList, Integer nextNode, Integer cost, Boolean isOn){
        boolean existed = false;
        for(Value v: valueList){
            if(v.getNextNode() == nextNode){
                v.setCost(cost);
                existed = true;
            }
        }
        if(!existed){
            Value newValue = new Value(nextNode, cost, isOn);
            valueList.add(newValue);
        }
    }

    // returns the smaller cost to reach a node, from alive nodes
    public int getBestCost(Integer destinyNode){
        return getBestValue(destinyNode).getCost();
    }

    public int getBestNextNode(Integer destinyNode){
        return getBestValue(destinyNode).getNextNode();
    }

    public Value getBestValue(Integer destinyNode){
        Value res = new Value();
        int min = Integer.MAX_VALUE;
        for(Value v: this.distanceVector.get(destinyNode))
            if(v.getIsOn() && min > v.getCost()){
                min = v.getCost();
                res = v;
            }
        return res;
    }


    public int getSpecificCost(Integer destinyNode, Integer nextNode){
        for(Value v: this.distanceVector.get(destinyNode)){
            if(v.getNextNode() == nextNode)
                return v.getCost();
        }
        return -1;

    }


    public HashMap<Integer, ArrayList<Value>> getDistanceVector() {
        return distanceVector;
    }


    public void setDistanceVector(HashMap<Integer, ArrayList<Value>> d) {
        this.distanceVector = d;
    }


    // ------------------------------ OTHER METHODS ------------------------------

    public boolean containsDestinyNode(Integer destinyNode){
        return this.distanceVector.containsKey(destinyNode);
    }

    @Override
    public String toString() {
        String res = "";
        for (Map.Entry<Integer, ArrayList<Value>> e : this.distanceVector.entrySet()){
            res += "destinyNodeId: " + e.getKey() +"\t"+ e.getValue().toString() + "\n";
        }
        return res;
    }
}
