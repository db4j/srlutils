// copyright nqzero 2017 - see License.txt for terms

package util;

import javax.swing.JFrame;

import com.mxgraph.layout.mxCompactTreeLayout;
import com.mxgraph.layout.mxGraphLayout;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.handler.mxKeyboardHandler;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;
import java.awt.BorderLayout;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashMap;
import org.srlutils.data.TreeDisk;
import org.srlutils.data.TreeDisk.Entry;
import org.srlutils.data.TreeDisk.Enview;

/**
 * a simple way to visualize data using jGraphX aka jgraph version 6
 * some inspiration from:
 *   http://videso3d.googlecode.com/svn/trunk/src/fr/crnan/videso3d/graphs/
 */

public class GraphViewer<XX> extends JFrame {
    public mxGraph graph;
    public mxGraphLayout layout;
    public mxGraphComponent graphComponent;
    public mxGraphModel model;
    public HashMap<XX, Object> hash = new HashMap();
    public Object parent;
    public mxKeyboardHandler keyboard;

    public GraphViewer() {
        super( "nqZero Graph Viewer" );
        model = new mxGraphModel();
        graph = new mxGraph( model );
        layout = new mxCompactTreeLayout( graph, false );
        graphComponent = new mxGraphComponent( graph );
        getContentPane().add( graphComponent );
        graphComponent.addMouseWheelListener(
            new MouseWheelListener() {
                public void mouseWheelMoved(MouseWheelEvent e) {
                    if ( e.getWheelRotation() < 0 ) graphComponent.zoomIn();
                    else graphComponent.zoomOut();
                }
            }
        );
        keyboard = new mxKeyboardHandler( graphComponent );

        this.getContentPane().add( graphComponent, BorderLayout.CENTER );
    }

    public void updateBegin() {
        hash.clear();
        model.clear();
        parent = graph.getDefaultParent();
        model.beginUpdate();
    }

    public void updateEnd() {
        model.endUpdate();
        layout.execute( graph.getDefaultParent() );
        getContentPane().repaint();
    }
    /** query the provider and start the viewer */
    public GraphViewer start() {
        setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
        setSize( 800, 700 );
        setVisible( true );
        return this;
    }

    public void node(XX entry,Object label,String color) {
        String style = "shape=ellipse;fillColor=white;strokeColor=" + color;
        Object cell = graph.insertVertex( parent, null, label, 0,0, 30,30, style );
        hash.put( entry, cell );
    }
    public void edge(XX entry,XX sub, String label) {
        Object cell1 = hash.get( entry );
        Object cell2 = hash.get( sub );
        graph.insertEdge( parent, null, label, cell1, cell2 );
    }

    public static class Viewer implements Enview {
        public GraphViewer gv;
        public void engraph(GraphViewer gv,Entry en) {
            if (!en.real()) return;
            gv.node(en, en.val, en.color==TreeDisk.BLACK ? "black" : "red" );
            Entry left = en.left, right = en.right;
            if (left .real()) { engraph(gv,left ); gv.edge(en, left,  "l"); }
            if (right.real()) { engraph(gv,right); gv.edge(en, right, "r"); }
        }
        public void graph(TreeDisk tree) {
            GraphViewer orig = gv;
            if (orig==null) gv =  new GraphViewer();
            gv.updateBegin();
            engraph(gv,tree.root);
            gv.updateEnd();
            if (orig==null) gv.start();
            org.srlutils.Simple.sleep(1000);
        }
    }

    public static void main(String[] args) {
        TreeDisk.ComparableSet<Integer> map = new TreeDisk.ComparableSet();
        Viewer view;
        map.view = view = new Viewer();
        // fixme -- occassional awt errors (race condition ???) during constant updates
        for (int ii = 0; ii < 10; ii++) { map.put( ii ); map.graph(); }

        org.srlutils.Simple.sleep( 5000 );
        
        // delete the 0 node and watch the tree rotate !!!
        Integer rem = map.remove( 0 );
        view.gv = null;
        map.graph();
    }

}
