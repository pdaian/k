package org.kframework.backend.java.kil;

import org.kframework.backend.java.symbolic.Matcher;
import org.kframework.backend.java.symbolic.Transformer;
import org.kframework.backend.java.symbolic.Unifier;
import org.kframework.backend.java.symbolic.Visitor;
import org.kframework.kil.ASTNode;


/**
 * Term representation of the operation of choosing a key from a map.
 *
 * @see org.kframework.backend.java.kil.BuiltinMap
 *
 * @author AndreiS
 */
public class MapKeyChoice extends Term {

    /**
     * Map from which the key is chosen.
     */
    private final Term map;

    public MapKeyChoice(Term map) {
        super(Kind.KITEM);
        this.map = map;
    }

    /**
     * Returns a non-deterministically chosen key from the map.
     * @return
     *     {@link org.kframework.backend.java.kil.Term} representation of a key if the map has entries
     *     {@link org.kframework.backend.java.kil.Bottom} is the map is empty (no entries and no frame)
     *     this object otherwise
     */
    public Term evaluateChoice() {
        if (!(map instanceof BuiltinMap)) {
            return this;
        }

        if (!((BuiltinMap) map).getEntries().isEmpty()) {
            return ((BuiltinMap) map).getEntries().keySet().iterator().next();
        } else if (((BuiltinMap) map).isEmpty()) {
            return new Bottom(kind);
        } else {
            return this;
        }
    }

    public Term map() {
        return map;
    }

    @Override
    public boolean isSymbolic() {
        return true;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof MapKeyChoice)) {
            return false;
        }

        MapKeyChoice mapKeyChoice = (MapKeyChoice) object;
        return map.equals(mapKeyChoice.map);
    }

    @Override
    public String toString() {
        return "choice(" + map.toString() + ")";
    }

    @Override
    public void accept(Unifier unifier, Term patten) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept(Matcher matcher, Term pattern) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ASTNode accept(Transformer transformer) {
        return transformer.transform(this);
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

}
