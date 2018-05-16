package guyThesis.wifiTranform.Utils;

/**
 * Created by Guy on 23/03/2015.
 * Taken from stack overflow
 */
public class Pair<A, B> {
    private A m_first;
    private B m_second;

    public Pair(A first, B second) {
        super();
        this.m_first = first;
        this.m_second = second;
    }

    public int hashCode() {
        int hashFirst = m_first != null ? m_first.hashCode() : 0;
        int hashSecond = m_second != null ? m_second.hashCode() : 0;

        return (hashFirst + hashSecond) * hashSecond + hashFirst;
    }

    public boolean equals(Object other) {
        if (other instanceof Pair) {
            Pair otherPair = (Pair) other;
            return
                    ((  this.m_first == otherPair.m_first ||
                            ( this.m_first != null && otherPair.m_first != null &&
                                    this.m_first.equals(otherPair.m_first))) &&
                            (	this.m_second == otherPair.m_second ||
                                    ( this.m_second != null && otherPair.m_second != null &&
                                            this.m_second.equals(otherPair.m_second))) );
        }
        return false;
    }

    public String toString()
    {
        return "(" + m_first + ", " + m_second + ")";
    }

    public A getFirst() {
        return m_first;
    }

    public void setFirst(A first) {
        this.m_first = first;
    }

    public B getSecond() {
        return m_second;
    }

    public void setSecond(B second) {
        this.m_second = second;
    }
}