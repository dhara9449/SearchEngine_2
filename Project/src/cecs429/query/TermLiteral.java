package cecs429.query;

import cecs429.index.Index;
import cecs429.index.Posting;
import cecs429.text.BetterTokenProcessor;

import java.util.List;

/**
 * A TermLiteral represents a single term in a sub query.
 */
public class TermLiteral implements QueryComponent {

    private final String mTerm;
    BetterTokenProcessor processor = new BetterTokenProcessor();//must be dynamic

    public TermLiteral(String term) {
        mTerm = processor.processToken(term).get(0);
    }

    public String getTerm() {
        return mTerm;
    }

    @Override
    public List<Posting> getPostings(Index index) {
        return index.getPostings(mTerm);
    }

    @Override
    public String toString() {
        return mTerm;
    }
}
