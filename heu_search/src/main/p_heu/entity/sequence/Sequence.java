package p_heu.entity.sequence;

import java.util.*;

import gov.nasa.jpf.vm.RestorableVMState;
import p_heu.entity.*;
import p_heu.entity.pattern.MemoryAccessPair;
import p_heu.entity.pattern.Pattern;

public class Sequence {
	private List<Node> nodes;
	private List<SearchState> states;
	private boolean finished;
	private boolean result;
	private int distance;
	private Set<Pattern> patterns;
	private Set<Sequence> correctSeqs;

	//distance的值是否过期
	private boolean consist;
	private boolean matched;
	
	public Sequence() {
		this.nodes = new ArrayList<>();
		this.states = new ArrayList<>();
		this.finished = false;
		this.result = false;
		this.distance = 0;
		this.consist = true;
		this.patterns = null;
		this.matched = false;
		this.correctSeqs = null;
	}

	public Sequence(Set<Sequence> correctSeqs) {
        this();
	    this.correctSeqs = correctSeqs;
    }

	private Sequence(List<Node> nodes, List<SearchState> states, boolean finished,
                     boolean result, int distance, boolean consist, Set<Pattern> patterns, boolean matched, Set<Sequence> correctSeqs) {
	    this.nodes = new ArrayList<>();
	    this.nodes.addAll(nodes);
        this.states = new ArrayList<>();
	    this.states.addAll(states);
	    this.finished = finished;
	    this.result = result;
	    this.distance = distance;
	    this.consist = consist;
	    this.patterns = patterns;
	    this.matched = matched;
	    this.correctSeqs = correctSeqs;
    }

	public Sequence copy() {
	    return new Sequence(
	            nodes, states, finished, result, distance, consist, patterns, matched, correctSeqs
        );
    }
	
	public List<Node> getNodes() {
		return this.nodes;
	}
	
//	public void addNode(Node node) {
//		this.nodes.add(node);
//	}
	
	public List<SearchState> getStates() {
		return this.states;
	}

	public SearchState getLastState() {
	    if (this.states.size() == 0) {
	        return null;
        }
        else {
            return this.states.get(this.states.size() - 1);
        }
    }
	
//	public void addState(SearchState state) {
//		this.states.add(state);
//	}
	
	public boolean isFinished() {
		return this.finished;
	}

	public boolean getResult() {
	    return this.result;
    }

//    public void setResult(boolean result) {
//	    this.result = result;
//	    if (this.finished) {
//	        throw new RuntimeException("Already a finished sequence.");
//        }
//        else {
//	        this.finished = true;
//        }
//    }

    public Sequence advance(int stateId, RestorableVMState state, List<Node> nodes) {
	    Sequence seq = this.copy();
	    seq.states.add(new SearchState(stateId, state));
	    seq.nodes.addAll(nodes);
	    seq.consist = false;
	    seq.matched = false;
	    return seq;
    }

    public Sequence advanceToEnd(int stateId, RestorableVMState state, List<Node> nodes, boolean result) {
	    Sequence seq = advance(stateId,state, nodes);
	    seq.finished = true;
	    seq.result = result;
	    return seq;
    }
	
	public int getDistance() {
	    if (consist) {
            return this.distance;
        }
        else {
	        return calculateDistance();
        }
	}

	private int calculateDistance() {
//        Random random = new Random();
//        this.distance = random.nextInt(10);
        if (correctSeqs == null) {
            throw new RuntimeException(
                    "correct sequence set is null: please use another constructor to create sequence or set correct sequence."
            );
        }

        int smallestSize = -1;
        for (Sequence correctSeq : correctSeqs) {
            Set<Pattern> differencePatterns = new HashSet<>();
            for (Pattern p : this.getPatterns()) {
                if (!correctSeq.isIn(p)) {
                    differencePatterns.add(p);
                }
            }
            if (smallestSize == -1 || differencePatterns.size() < smallestSize) {
                smallestSize = differencePatterns.size();
            }
        }
        this.distance = smallestSize;

        this.consist = true;
        return this.distance;
    }

    //当correctSeqs发生变动时，需要调用，使distance保持更新
    public void distanceNeedUpdate() {
	    this.consist = false;
    }

    public Set<Pattern> getPatterns() {
	    if (matched) {
	        return this.patterns;
        }
        else {
	        this.patterns = matchPatterns(Pattern.getPatternSet());
	        this.matched = true;
	        return this.patterns;
        }
    }

    public void setCorrectSeqs(Set<Sequence> correctSeqs) {
	    this.correctSeqs = correctSeqs;
    }

    public boolean isIn(Pattern pattern) {
	    Set<Pattern> patterns = this.getPatterns();
	    for (Pattern p : patterns) {
	        if (p.isSamePattern(pattern)) {
	            return true;
            }
        }
        return false;
    }
	
	public Set<Pattern> matchPatterns(String patternSet) {
	    Set<Pattern> result = new HashSet<>();
        MemoryAccessPair[] pairs = Pattern.getMemoryAccessPairs();
        List<MemoryAccessPair> matchedPairs = matchPairs(pairs);
        result.addAll(matchedPairs);
        for (int i = 0; i < matchedPairs.size(); ++i) {
            for (int j = i + 1; j < matchedPairs.size(); ++j) {
                MemoryAccessPair pair1 = matchedPairs.get(i);
                MemoryAccessPair pair2 = matchedPairs.get(j);
                Pattern pattern = null;
                if (patternSet.equals("falcon")) {
                    pattern = Pattern.tryConstructFalconPattern(pair1, pair2);
                }
                else if (patternSet.equals("unicorn")) {
                    pattern = Pattern.tryConstrucUnicornPattern(pair1, pair2);
                }
                else {
                    throw new RuntimeException("unknown pattern set");
                }
                if (pattern != null) {
                    result.add(pattern);
                }
            }
        }
		return result;
	}

	private List<MemoryAccessPair> matchPairs(MemoryAccessPair[] pairs) {
	    List<MemoryAccessPair> result = new ArrayList<>();
	    for (int i = 0; i < this.nodes.size(); ++i) {
	        if (this.nodes.get(i) instanceof ReadWriteNode) {
	            ReadWriteNode node = (ReadWriteNode)this.nodes.get(i);
	            result.addAll(matchNext(node, i + 1, pairs));
            }
        }
        return result;
    }

    private Set<MemoryAccessPair> matchNext(ReadWriteNode node, int begin, MemoryAccessPair[] pairs) {
	    Set<MemoryAccessPair> matchedPairs = new HashSet<>();
	    for (int i = begin; i < this.nodes.size(); ++i) {
	        if (!(this.nodes.get(i) instanceof ReadWriteNode)) {
	            continue;
            }
            ReadWriteNode nextNode = (ReadWriteNode)this.nodes.get(i);
	        if (!node.isSameInstance(nextNode)) {
	            continue;
            }
	        MemoryAccessPair matchedPair = isMatch(node, nextNode, pairs);
	        if (matchedPair != null) {
	            matchedPairs.add(matchedPair);
            }
            if (nextNode.getType().equals("WRITE")) {
	            break;
            }
        }
        return matchedPairs;
    }

    private MemoryAccessPair isMatch(ReadWriteNode node1, ReadWriteNode node2, MemoryAccessPair[] pairs) {
	    //是否是同一个变量已经在上一个函数检查，这里不需要

	    for (MemoryAccessPair pair : pairs) {
	        //检查线程是否与pair要求一致
            PatternTypeNode[] ptNode = pair.getPatternType().getNodes();
	        if (node1.getThread().equals(node2.getThread()) != ptNode[0].getThread().equals(ptNode[1].getThread())) {
	            continue;
            }

            //检查读写种类是否一致
            if (node1.getType().equals(ptNode[0].getType()) && node2.getType().equals(ptNode[1].getType())) {
	            return new MemoryAccessPair(pair.getPatternType(), new ReadWriteNode[] {
	                    node1, node2
                });
            }
        }
        return null;
    }


	public String toString() {
	    StringBuilder stringBuilder = new StringBuilder("Sequence {\n");
        stringBuilder.append("\tnodes:\n");
        for (Node node : nodes) {
            stringBuilder.append("\t");
            stringBuilder.append(node);
            stringBuilder.append("\n");
        }
        stringBuilder.append("\tstates:\n");
        for (SearchState state : states) {
            stringBuilder.append("\t");
            stringBuilder.append(state);
            stringBuilder.append("\n");
        }
        stringBuilder.append("\tfinished: ");
        stringBuilder.append(finished);
        stringBuilder.append("\n\tresult: ");
        stringBuilder.append(result);
        stringBuilder.append("\n}");
	    return stringBuilder.toString();
    }
}
