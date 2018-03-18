package fix.analyzefile;

import fix.entity.record.MatchResult;
import fix.entity.record.NodeSequence;
import p_heu.entity.Node;
import p_heu.entity.ReadWriteNode;
import p_heu.listener.SequenceProduceListener;

import java.util.ArrayList;
import java.util.List;

//这个类对Sequence操作
public class RecordSequence {

    //存放所有的NodeSequence
    static List<NodeSequence> nodeSequenceList = new ArrayList<NodeSequence>();

    //所有读写节点
    static List<ReadWriteNode> readWriteNodeList = new ArrayList<ReadWriteNode>();

    public static List<NodeSequence> getNodeSequenceList() {
        return nodeSequenceList;
    }

    public static List<ReadWriteNode> getReadWriteNodeList() {
        return readWriteNodeList;
    }

    public static void analyseReadWriteNodeList(List<Node> nodesList) {
        for (Node node : nodesList) {
            if (node instanceof ReadWriteNode) {
                readWriteNodeList.add((ReadWriteNode) node);
            }
        }
    }

    public static void display(SequenceProduceListener listener) {
        System.out.println("nodes信息");
        List<Node> nodesList = listener.getSequence().getNodes();
        for (Node node : nodesList) {
            if (node instanceof ReadWriteNode) {
                System.out.println(node);
                dealwith((ReadWriteNode) node);
            }
        }

        //得到所有readwritenode信息，这里
        // 只为了关联变量考虑，其实还可以优化
        //比如将读写节点和后面的分类结合
        analyseReadWriteNodeList(nodesList);

        //遍历输出
        System.out.println("遍历输出");
        for (NodeSequence nodeSequence : nodeSequenceList) {
            System.out.println(nodeSequence.getElement() + "," + nodeSequence.getField() + "," + nodeSequence.getThread() + "," + nodeSequence.getPosition());
            for (int i : nodeSequence.getIdList()) {
                System.out.print(i + " ");
            }
            System.out.println("=============");
        }
    }

    //将sequence里面的数据，按照线程和行数分类
    private static void dealwith(ReadWriteNode node) {
        NodeSequence nodeSequence = new NodeSequence(node.getElement(), node.getField(), node.getThread(), node.getPosition());
        //先判断list中有没有，没有，则添加，有，直接找到有的那个，在它的arr里面添加
        MatchResult mr = listMatch(nodeSequence);
        if (mr.isFlag()) {
            nodeSequenceList.get(mr.getIndex()).add(node.getId());
        } else {
            nodeSequence.add(node.getId());
            nodeSequenceList.add(nodeSequence);
        }
    }

    private static MatchResult listMatch(NodeSequence ns) {
        MatchResult matchResult = new MatchResult();
        for (int i = 0; i < nodeSequenceList.size(); i++) {
            NodeSequence nodeS = nodeSequenceList.get(i);
            if (nodeS.getElement().equals(ns.getElement()) && nodeS.getField().equals(ns.getField()) && nodeS.getPosition().equals(ns.getPosition()) && nodeS.getThread().equals(ns.getThread())) {
                matchResult.setFlag(true);
                matchResult.setIndex(i);
            }
        }
        return matchResult;
    }

    public static boolean isLast(ReadWriteNode readWriteNode) {
        for (int i = 0; i < nodeSequenceList.size(); i++) {
            NodeSequence nodeS = nodeSequenceList.get(i);
            if (nodeS.getElement().equals(readWriteNode.getElement()) && nodeS.getField().equals(readWriteNode.getField()) && nodeS.getPosition().equals(readWriteNode.getPosition()) && nodeS.getThread().equals(readWriteNode.getThread())) {
                int index = nodeS.getIdList().indexOf(readWriteNode.getId());
                if ((index + 1) == nodeS.getIdList().size())
                    return true;
            }
        }
        return false;
    }

    public static boolean isFirst(ReadWriteNode readWriteNode) {
        for (int i = 0; i < nodeSequenceList.size(); i++) {
            NodeSequence nodeS = nodeSequenceList.get(i);
            if (nodeS.getElement().equals(readWriteNode.getElement()) && nodeS.getField().equals(readWriteNode.getField()) && nodeS.getPosition().equals(readWriteNode.getPosition()) && nodeS.getThread().equals(readWriteNode.getThread())) {
                int index = nodeS.getIdList().indexOf(readWriteNode.getId());
                if (index == 0)
                    return true;
            }
        }
        return false;
    }
}
