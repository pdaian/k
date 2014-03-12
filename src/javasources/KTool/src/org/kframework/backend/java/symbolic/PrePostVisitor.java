package org.kframework.backend.java.symbolic;

import org.kframework.backend.java.builtins.BoolToken;
import org.kframework.backend.java.builtins.IntToken;
import org.kframework.backend.java.builtins.Int32Token;
import org.kframework.backend.java.builtins.UninterpretedToken;
import org.kframework.backend.java.kil.*;

import java.util.Map;


/**
 * A bottom-up implementation of the visitor pattern.
 *
 * @author Traian
 */
public class PrePostVisitor implements Visitor {

    public CombinedLocalVisitor getPreVisitor() {
        return preVisitor;
    }

    public void setPreVisitor(CombinedLocalVisitor preVisitor) {
        this.preVisitor = preVisitor;
    }

    public CombinedLocalVisitor getPostVisitor() {
        return postVisitor;
    }

    public void setPostVisitor(CombinedLocalVisitor postVisitor) {
        this.postVisitor = postVisitor;
    }

    CombinedLocalVisitor preVisitor = new CombinedLocalVisitor();
    CombinedLocalVisitor postVisitor = new CombinedLocalVisitor();

    @Override
    public String getName() {
        return this.getClass().toString();
    }

    @Override
    public void visit(BuiltinMap builtinMap) {
        preVisitor.resetProceed();
        builtinMap.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        for (Map.Entry<Term, Term> entry : builtinMap.getEntries().entrySet()) {
            entry.getKey().accept(this);
            entry.getValue().accept(this);
        }
        if (builtinMap.hasFrame()) {
            builtinMap.frame().accept(this);
        }
        builtinMap.accept(postVisitor);
    }

    @Override
    public void visit(BuiltinSet builtinSet) {
        preVisitor.resetProceed();
        builtinSet.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        for (Term term : builtinSet.elements()) {
            term.accept(this);
        }
        if (builtinSet.hasFrame()) {
            builtinSet.frame().accept(this);
        }
        builtinSet.accept(postVisitor);
    }

    @Override public void visit(Term term) {
        preVisitor.resetProceed();
        term.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        term.accept(postVisitor);
    }

    @Override
    public void visit(Cell cell) {
        preVisitor.resetProceed();
        cell.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        cell.getContent().accept(this);
        cell.accept(postVisitor);
    }

    @Override
    public void visit(CellCollection cellCollection) {
        preVisitor.resetProceed();
        cellCollection.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        for (Cell<?> cell : cellCollection.cells()) {
            cell.accept(this);
        }
        if (cellCollection.hasFrame()) {
            cellCollection.frame().accept(this);
        }
        cellCollection.accept(postVisitor);
    }

    @Override
    public void visit(Collection collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void visit(Hole hole) {
        preVisitor.resetProceed();
        hole.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        hole.accept(postVisitor);
    }

    @Override
    public void visit(KLabelConstant kLabelConstant) {
        preVisitor.resetProceed();
        kLabelConstant.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        kLabelConstant.accept(postVisitor);
    }

    @Override
    public void visit(KLabelFreezer kLabelFreezer) {
        preVisitor.resetProceed();
        kLabelFreezer.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        kLabelFreezer.term().accept(this);
        kLabelFreezer.accept(postVisitor);
    }

    @Override
    public void visit(KLabelInjection kLabelInjection) {
        preVisitor.resetProceed();
        kLabelInjection.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        kLabelInjection.term().accept(this);
        kLabelInjection.accept(postVisitor);
    }

    @Override
    public void visit(KItem kItem) {
        preVisitor.resetProceed();
        kItem.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        kItem.kLabel().accept(this);
        kItem.kList().accept(this);
        kItem.accept(postVisitor);
    }

    @Override
    public void visit(KItemProjection kItemProjection) {
        preVisitor.resetProceed();
        kItemProjection.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        kItemProjection.term().accept(this);
        kItemProjection.accept(postVisitor);
    }

    @Override
    public void visit(Token token) {
        preVisitor.resetProceed();
        token.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        token.accept(postVisitor);
    }

    @Override
    public void visit(UninterpretedConstraint uninterpretedConstraint) {
        preVisitor.resetProceed();
        uninterpretedConstraint.accept(preVisitor);
        if (!preVisitor.isProceed()) return;

        for (UninterpretedConstraint.Equality equality : uninterpretedConstraint.equalities()) {
            equality.leftHandSide().accept(this);
            equality.rightHandSide().accept(this);
        }

        uninterpretedConstraint.accept(postVisitor);
    }

    @Override
    public void visit(UninterpretedToken uninterpretedToken) {
        preVisitor.resetProceed();
        uninterpretedToken.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        uninterpretedToken.accept(postVisitor);
    }

    @Override
    public void visit(BoolToken boolToken) {
        preVisitor.resetProceed();
        boolToken.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        boolToken.accept(postVisitor);
    }

    @Override
    public void visit(IntToken intToken) {
        preVisitor.resetProceed();
        intToken.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        intToken.accept(postVisitor);
    }

    @Override
    public void visit(Int32Token intToken) {
        preVisitor.resetProceed();
        intToken.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        intToken.accept(postVisitor);
    }

    @Override
    public void visit(KCollection kCollection) {
        preVisitor.resetProceed();
        kCollection.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        for (Term term : kCollection) {
            term.accept(this);
        }
        if (kCollection.hasFrame())
            kCollection.frame().accept(this);
        kCollection.accept(postVisitor);
    }

    @Override
    public void visit(KCollectionFragment kCollectionFragment) {
        preVisitor.resetProceed();
        kCollectionFragment.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        for (Term term : kCollectionFragment) {
            term.accept(this);
        }
        if (kCollectionFragment.hasFrame())
            kCollectionFragment.frame().accept(this);
        kCollectionFragment.accept(postVisitor);
    }

    @Override
    public void visit(KLabel kLabel) {
        preVisitor.resetProceed();
        kLabel.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        kLabel.accept(postVisitor);
    }

    @Override
    public void visit(KList kList) {
        visit((KCollection) kList);
    }

    @Override
    public void visit(KSequence kSequence) {
        visit((KCollection) kSequence);
    }

    @Override
    public void visit(MapKeyChoice mapKeyChoice) {
        preVisitor.resetProceed();
        mapKeyChoice.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        mapKeyChoice.map().accept(this);
        mapKeyChoice.accept(postVisitor);
    }

    @Override
    public void visit(MapLookup mapLookup) {
        preVisitor.resetProceed();
        mapLookup.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        mapLookup.map().accept(this);
        mapLookup.key().accept(this);
        mapLookup.accept(postVisitor);
    }

    @Override
    public void visit(MapUpdate mapUpdate) {
        preVisitor.resetProceed();
        mapUpdate.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        mapUpdate.map().accept(this);
        for (Term key : mapUpdate.removeSet()) {
            key.accept(this);
        }
        for (Map.Entry<Term, Term> entry : mapUpdate.updateMap().entrySet()) {
            entry.getKey().accept(this);
            entry.getValue().accept(this);
        }
        mapUpdate.accept(postVisitor);
    }

    @Override
    public void visit(SetElementChoice setElementChoice) {
        preVisitor.resetProceed();
        setElementChoice.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        setElementChoice.set().accept(this);
        setElementChoice.accept(postVisitor);
    }

    @Override
    public void visit(SetLookup setLookup) {
        preVisitor.resetProceed();
        setLookup.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        setLookup.base().accept(this);
        setLookup.key().accept(this);
        setLookup.accept(postVisitor);
    }

    @Override
    public void visit(SetUpdate setUpdate) {
        preVisitor.resetProceed();
        setUpdate.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        setUpdate.base().accept(this);
        for (Term key : setUpdate.removeSet()) {
            key.accept(this);
        }
        setUpdate.accept(postVisitor);
    }

    @Override
    public void visit(MetaVariable metaVariable) {
        visit((Token) metaVariable);
    }

    @Override
    public void visit(Rule rule) {
        preVisitor.resetProceed();
        rule.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        rule.leftHandSide().accept(this);
        rule.rightHandSide().accept(this);
        rule.lookups().accept(this);
        for (Term term : rule.requires()) {
            term.accept(this);
        }
        for (Term term : rule.ensures()) {
            term.accept(this);
        }
        for (Variable variable : rule.freshVariables()) {
            variable.accept(this);
        }
        rule.accept(postVisitor);
    }

    @Override
    public void visit(SymbolicConstraint node) {
        preVisitor.resetProceed();
        node.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        for (Map.Entry<Variable, Term> entry : node.substitution().entrySet()) {
            entry.getKey().accept(this);
            entry.getValue().accept(this);
        }
        for (SymbolicConstraint.Equality equality : node.equalities()) {
            equality.leftHandSide().accept(this);
            equality.rightHandSide().accept(this);
        }
        node.accept(postVisitor);
    }

    @Override
    public void visit(Variable variable) {
        visit((Term) variable);
    }

    @Override
    public void visit(ListLookup node) {
        preVisitor.resetProceed();
        node.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        node.list().accept(this);
        node.key().accept(this);
        node.accept(postVisitor);
    }

    @Override
    public void visit(ConstrainedTerm node) {
        // TODO(Traian): check if this fix is correct
        preVisitor.resetProceed();
        node.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        node.term().accept(this);
        node.lookups().accept(this);
        node.constraint().accept(this);
        node.accept(postVisitor);
    }

    @Override
    public void visit(BuiltinList node) {
        preVisitor.resetProceed();
        node.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        if (node.hasFrame()) node.frame().accept(this);
        for (Term t : node.elementsLeft()) t.accept(this);
        for (Term t : node.elementsRight()) t.accept(this);
        node.accept(postVisitor);
    }

    @Override
    public void visit(BuiltinMgu mgu) {
        preVisitor.resetProceed();
        mgu.accept(preVisitor);
        if (!preVisitor.isProceed()) return;
        mgu.constraint().accept(this);
        mgu.accept(postVisitor);
    }
}
