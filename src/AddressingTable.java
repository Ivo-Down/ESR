import java.util.HashMap;

public class AddressingTable {

    class MapValue {
        Integer nextNode;
        Integer cost ;
        public MapValue(Integer nextNode , Integer cost)  // key: nextNodeId    value: cost
        {
            this.nextNode= nextNode;
            this.cost=cost;
        }

        public int getCost(){
            return this.cost;
        }
    }


    private HashMap<Integer, MapValue> distanceVector;  // key: destinyNodeId   value: pair of nextNodeId and cost



    public AddressingTable(){
        this.distanceVector = new HashMap<>();
        //this.setDistance(nodeId, nodeId, 0); // o custo para chegar a ele mesmo é 0
    }




    // ------------------------------ GETTERS AND SETTERS ------------------------------

    public void setDistance(Integer destinyNode, Integer nextNode, Integer cost){
        MapValue mapValue = new MapValue(nextNode, cost);
        this.distanceVector.put(destinyNode, mapValue);
    }

    public int getDistance(Integer destinyNode){
        return this.distanceVector.get(destinyNode).getCost();
    }

    public HashMap<Integer, MapValue> getDistanceVector() {
        return distanceVector;
    }



    // ------------------------------ OTHER METHODS ------------------------------

    public boolean containsNode(Integer nodeId){
        return this.distanceVector.containsKey(nodeId);
    }
}
