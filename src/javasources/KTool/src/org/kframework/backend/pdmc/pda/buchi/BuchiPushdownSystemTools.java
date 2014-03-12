package org.kframework.backend.pdmc.pda.buchi;

import org.apache.commons.lang3.tuple.Pair;
import org.kframework.backend.pdmc.automaton.Transition;
import org.kframework.backend.pdmc.pda.Configuration;
import org.kframework.backend.pdmc.pda.ConfigurationHead;
import org.kframework.backend.pdmc.pda.Rule;
import org.kframework.backend.pdmc.pda.graph.TarjanSCC;
import org.kframework.backend.pdmc.pda.pautomaton.PAutomaton;
import org.kframework.backend.pdmc.pda.pautomaton.PAutomatonState;
import org.kframework.backend.pdmc.pda.pautomaton.util.IndexedTransitions;

import java.util.*;

/**
 * Implements some pushdown model-checking related algorithms from  Stefan Schwoon's Phd Thesis:
 * S. Schwoon.  Model-Checking Pushdown Systems.  Ph.D. Thesis, Technische Universitaet Muenchen, June 2002.
 *
 * @author TraianSF
 */
public class BuchiPushdownSystemTools<Control, Alphabet> {

    /**
     * Implements labeled transitions for the Post*  reachability automate.
     * Labels are added on the letters as specified in Schwoon's thesis,
     * Sec. 3.1.6 Witness generation.
     *
     * @param <Control>  specifies the control state of a pushdwown system
     * @param <Alphabet> specifies the alphabet of a pushdown system
     */
    static class LabelledAlphabet<Control, Alphabet> {
        Alphabet letter;
        //whether this transition corresponds to a repeated head in the pds
        boolean repeated;

        public Rule<Pair<Control, BuchiState>, Alphabet> getRule() {
            return rule;
        }

        public void setRule(Rule<Pair<Control, BuchiState>, Alphabet> rule) {
            this.rule = rule;
        }

        public PAutomatonState<Pair<Control, BuchiState>, Alphabet> getBackState() {
            return backState;
        }

        public void setBackState(PAutomatonState<Pair<Control, BuchiState>, Alphabet> backState) {
            this.backState = backState;
        }

        // the rule used to label this transition
        Rule<Pair<Control, BuchiState>, Alphabet> rule;
        // if the transition is due to an epsilon transition identify the intermediate state.
        PAutomatonState<Pair<Control, BuchiState>, Alphabet> backState;

        LabelledAlphabet(Alphabet letter, boolean repeated) {
            this.letter = letter;
            this.repeated = repeated;
            rule = null;
            backState = null;
        }

        public static<Control, Alphabet> LabelledAlphabet<Control, Alphabet> of(Alphabet letter, boolean repeated) {
           return new LabelledAlphabet<>(letter, repeated);
        }

        public Alphabet getLeft() {
            return letter;
        }

        public boolean isRepeated() {
            return repeated;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            LabelledAlphabet that = (LabelledAlphabet) o;

            if (repeated != that.repeated) return false;
            if (letter != null ? !letter.equals(that.letter) : that.letter != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = letter != null ? letter.hashCode() : 0;
            result = 31 * result + (repeated ? 1 : 0);
            return result;
        }

        @Override
        public String toString() {
            return "<" +
                    "" + letter +
                    ", " + repeated +
                    '>';
        }

        public void update(LabelledAlphabet<Control, Alphabet> newLabel) {
            repeated |= newLabel.repeated;
        }
    }

    BuchiPushdownSystemInterface<Control, Alphabet> bps;

    public BuchiPushdownSystemTools(BuchiPushdownSystemInterface<Control, Alphabet> bps) {
        this.bps = bps;
    }

    /**
     * Class used to track epsilon transitions for the Repeated heads graph generation
     */
    class EpsilonTransitionWatch {
        Map<PAutomatonState<Pair<Control, BuchiState>, Alphabet>,
                Set<Pair<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>,
                        LabelledAlphabet<Control, Alphabet>>>> transitionsWatchMap;

        EpsilonTransitionWatch() {
            this.transitionsWatchMap = new HashMap<>();
        }

        Set<Pair<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>,
                LabelledAlphabet<Control, Alphabet>>> get(PAutomatonState<Pair<Control, BuchiState>, Alphabet> key) {
            Set<Pair<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, LabelledAlphabet<Control, Alphabet>>> set =
                    transitionsWatchMap.get(key);
            if (set == null) {
                set = Collections.emptySet();
            }
            return set;
        }

        void addWatch(PAutomatonState<Pair<Control, BuchiState>, Alphabet> key,
                      ConfigurationHead<Pair<Control, BuchiState>, Alphabet> startHead,
                      Alphabet endLetter,
                      Rule<Pair<Control, BuchiState>, Alphabet> rule) {
            Set<Pair<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, LabelledAlphabet<Control, Alphabet>>> set =
                    transitionsWatchMap.get(key);
            if (set == null) {
                set = new HashSet<>();
                transitionsWatchMap.put(key, set);
            }
            LabelledAlphabet<Control, Alphabet> labelledLetter = LabelledAlphabet.of(endLetter, false);
            labelledLetter.setBackState(key);
            labelledLetter.setRule(rule);
            set.add(Pair.of(startHead, labelledLetter));
        }
    }

    private PAutomaton<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> postStar = null;
    TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>,
            LabelledAlphabet<Control, Alphabet>> repeatedHeadsGraph = null;
    Map<Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet>, LabelledAlphabet<Control, Alphabet>>
            transitionLabels = null;

    TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, LabelledAlphabet<Control, Alphabet>> counterExample = null;

    public PAutomaton<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> getPostStar() {
        if (postStar == null)
            compute();
        return postStar;
    }

    public TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, LabelledAlphabet<Control, Alphabet>> getRepeatedHeadsGraph() {
        if (repeatedHeadsGraph == null)
            compute();
        return repeatedHeadsGraph;
    }

    public TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, LabelledAlphabet<Control, Alphabet>> getCounterExample() {
        if (repeatedHeadsGraph == null)
            compute();
        return counterExample;
    }

    /**
     * Main method of the class. Implements the post* algorithm instrumented to also produce the repeated heads graph.
     * The post* algorithm implemented is presented in Figure 3.4, Section 3.1.4 of S. Schwoon's PhD thesis (p. 48)
     * The modification to compute the repeated heads graph is explained in Section 3.2.3 of Schwoon's thesis
     * (see also Algorithm 4 in Figure 3.9, p. 81)
     */
    private void compute() {
        transitionLabels = new HashMap<>();
        EpsilonTransitionWatch watch = new EpsilonTransitionWatch();
        Set<Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet>> trans =
                new HashSet<>();
        IndexedTransitions<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> rel =
                new IndexedTransitions<>();

        repeatedHeadsGraph = new TarjanSCC<>();

        Configuration<Pair<Control, BuchiState>, Alphabet> initial = bps.initialConfiguration();
        ConfigurationHead<Pair<Control, BuchiState>, Alphabet> initialHead = initial.getHead();
        PAutomatonState<Pair<Control, BuchiState>, Alphabet> initialState =
                PAutomatonState.of(initialHead.getState());
        assert initial.getStack().isEmpty() : "Only one element in the initial stack accepted at the moment";
        PAutomatonState<Pair<Control, BuchiState>, Alphabet> finalState =
                PAutomatonState.of(initialHead.getState(), initialHead.getLetter());
        Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> transition1 =
                Transition.of(initialState, initialHead.getLetter(), finalState);
        trans.add(transition1);
        LabelledAlphabet<Control, Alphabet> newLabel = LabelledAlphabet.of(initialHead.getLetter(), false);
        updateLabel(transition1, newLabel);
        LabelledAlphabet<Control, Alphabet> labelledLetter;

        while (!trans.isEmpty()) {
            Iterator<Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet>> iterator
                    = trans.iterator();
            Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> transition = iterator.next();
            iterator.remove();
            Alphabet letter = transition.getLetter();
            LabelledAlphabet<Control, Alphabet> oldLabel = transitionLabels.get(transition);
            assert oldLabel != null : "Each transition must have a label";
            if (letter == null) {
                for (Pair<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>,
                        LabelledAlphabet<Control, Alphabet>> pair : watch.get(transition.getEnd())) {
                    ConfigurationHead<Pair<Control, BuchiState>, Alphabet> endV =
                            ConfigurationHead.of(transition.getStart().getState(), pair.getRight().letter);
                    labelledLetter = new LabelledAlphabet<>(pair.getRight().letter,
                            pair.getRight().isRepeated() || oldLabel.isRepeated());
                    labelledLetter.setRule(pair.getRight().getRule());
                    labelledLetter.setBackState(pair.getRight().getBackState());
                    repeatedHeadsGraph.addEdge(pair.getLeft(), endV, labelledLetter);
                }

            }
            if (!rel.contains(transition)) {
                Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> newTransition;
                rel.add(transition);
                Alphabet gamma = letter;
                boolean b = oldLabel.isRepeated();
                PAutomatonState<Pair<Control, BuchiState>, Alphabet> tp = transition.getStart();
                PAutomatonState<Pair<Control, BuchiState>, Alphabet> q = transition.getEnd();
                if (gamma != null) {
                    assert tp.getLetter() == null : "Expecting PDS state on the lhs of " + transition;
                    Pair<Control, BuchiState> p = tp.getState();
                    final ConfigurationHead<Pair<Control, BuchiState>, Alphabet> configurationHead
                            = ConfigurationHead.of(p, gamma);
                    Set<Rule<Pair<Control, BuchiState>, Alphabet>> rules =
                            bps.getRules(configurationHead);
                    for (Rule<Pair<Control, BuchiState>, Alphabet> rule : rules) {
                        Pair<Control, BuchiState> pPrime = rule.endState();
                        Stack<Alphabet> stack = rule.endStack();
                        assert stack.size() <= 2 : "At most 2 elements are allowed in the stack for now";
                        Alphabet gamma1, gamma2;
                        switch (stack.size()) {
                            case 0:
                                labelledLetter = LabelledAlphabet.of(null, b || bps.isFinal(pPrime));
                                labelledLetter.setRule(rule);
                                newTransition = Transition.of(PAutomatonState.<Pair<Control, BuchiState>, Alphabet>of(pPrime),
                                        null, q);
                                trans.add(newTransition);
                                updateLabel(newTransition, labelledLetter);
                                break;
                            case 1:
                                gamma1 = stack.peek();
                                labelledLetter = LabelledAlphabet.of(gamma1,
                                        b || bps.isFinal(pPrime));
                                labelledLetter.setRule(rule);
                                newTransition = Transition.of(
                                        PAutomatonState.<Pair<Control, BuchiState>, Alphabet>of(pPrime),
                                        gamma1, q);
                                trans.add(newTransition);
                                labelledLetter = updateLabel(newTransition, labelledLetter);
                                repeatedHeadsGraph.addEdge(configurationHead,
                                        rule.endConfiguration().getHead(),
                                        labelledLetter);
                                break;
                            case 2:
                                gamma1 = stack.get(1);
                                gamma2 = stack.get(0);
                                PAutomatonState<Pair<Control, BuchiState>, Alphabet> qPPrimeGamma1
                                        = PAutomatonState.of(pPrime, gamma1);
                                labelledLetter = LabelledAlphabet.of(gamma1,
                                        b || bps.isFinal(pPrime));
                                labelledLetter.setRule(rule);
                                newTransition = Transition.of(
                                        PAutomatonState.<Pair<Control, BuchiState>, Alphabet>of(pPrime),
                                        gamma1, qPPrimeGamma1);
                                trans.add(newTransition);
                                updateLabel(newTransition, labelledLetter);
                                labelledLetter = LabelledAlphabet.of(gamma2, false);
                                labelledLetter.setRule(rule);
                                newTransition = Transition.of(qPPrimeGamma1, gamma2, q);
                                rel.add(newTransition);
                                labelledLetter = updateLabel(newTransition, labelledLetter);
                                repeatedHeadsGraph.addEdge(configurationHead,
                                        rule.endConfiguration().getHead(),
                                        labelledLetter);
                                for (Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> t
                                        : rel.getBackEpsilonTransitions(qPPrimeGamma1)) {
                                    oldLabel = transitionLabels.get(t);
                                    labelledLetter = LabelledAlphabet.of(gamma2, oldLabel.isRepeated());
                                    labelledLetter.setRule(rule);
                                    labelledLetter.setBackState(qPPrimeGamma1);
                                    newTransition = Transition.of(t.getStart(), gamma2, q);
                                    trans.add(newTransition);
                                    labelledLetter = updateLabel(newTransition, labelledLetter);
                                    ConfigurationHead<Pair<Control, BuchiState>, Alphabet> endV =
                                            ConfigurationHead.of(t.getStart().getState(), gamma2);
                                    repeatedHeadsGraph.addEdge(configurationHead, endV, labelledLetter);
                                }
                                watch.addWatch(qPPrimeGamma1, configurationHead, gamma2, rule);
                        }
                    }
                } else {
                    for (Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> t
                            : rel.getFrontTransitions(q)) {
                        LabelledAlphabet<Control, Alphabet> tLetter = transitionLabels.get(t);
                        labelledLetter = LabelledAlphabet.of(
                                tLetter.getLeft(),
                                tLetter.isRepeated() || b);
                        labelledLetter.setBackState(q);
                        newTransition = Transition.of(tp, tLetter.getLeft(), t.getEnd());
                        trans.add(newTransition);
                        updateLabel(newTransition, labelledLetter);
                    }
                }
            }
        }

        postStar = new PAutomaton<>(
                rel.getTransitions(),
                initialState,
                Collections.singleton(finalState));

        computeCounterExample();

    }

    private LabelledAlphabet<Control, Alphabet> updateLabel(Transition<PAutomatonState<Pair<Control, BuchiState>, Alphabet>, Alphabet> transition, LabelledAlphabet<Control, Alphabet> label) {
        LabelledAlphabet<Control, Alphabet> labelledLetter = transitionLabels.get(transition);
        if (labelledLetter == null) {
            transitionLabels.put(transition, label);
            labelledLetter = label;
        } else {
            labelledLetter.update(label);
        }
        transition.setLabel(labelledLetter);
        return labelledLetter;
    }

    private void computeCounterExample() {
        Collection<Collection<TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, LabelledAlphabet<Control, Alphabet>>.TarjanSCCVertex>> sccs = repeatedHeadsGraph.getStronglyConnectedComponents();
        for (Collection<TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, LabelledAlphabet<Control, Alphabet>>.TarjanSCCVertex> scc : sccs) {
            TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, LabelledAlphabet<Control, Alphabet>> sccSubGraph = repeatedHeadsGraph.getSubgraph(scc);
            for (Map<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, LabelledAlphabet<Control, Alphabet>> values : sccSubGraph.getEdgeSet().values()) {
                for (LabelledAlphabet<Control, Alphabet> label: values.values()) {
                    if (label.isRepeated()) {
                        counterExample = sccSubGraph;
                        return;
                    }
                }
            }
        }
    }

    private Configuration<Pair<Control, BuchiState>, Alphabet> getReachableConfiguration(
           TarjanSCC<ConfigurationHead<Pair<Control, BuchiState>, Alphabet>, LabelledAlphabet<Control, Alphabet>>.TarjanSCCVertex v
    ) {
        ConfigurationHead<Pair<Control, BuchiState>, Alphabet> head = v.getData();
//        postStar.getTransitions(PAutomatonState.<Pair<Control, BuchiState>, Alphabet>of(head.getState()), head.getLetter());
        return null;
    }

}
