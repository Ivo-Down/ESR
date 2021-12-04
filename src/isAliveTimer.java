import java.util.TimerTask;

public class isAliveTimer extends TimerTask {
    private Table overlayNodes;

    public isAliveTimer(Table overlayNodes) {
        this.overlayNodes = overlayNodes;
    }

    @Override
    public void run() {
        // Get nodes that are flagged with 'ON'
        Table onlineNodes = this.overlayNodes.getNodesWithState(NodeInfo.nodeState.ON);
    }
}