package sample;

import javafx.util.Pair;
import java.util.*;

public class Ranker {

    //<editor-fold desc="vars">
    Hashtable<String,Hashtable<String,Integer>> FTable;
    Indexer indexer;
    boolean do_entities;
    boolean do_semantic;
    ArrayList<String> query;
    ArrayList<String> records;
    HashSet<String> docs;
    double k1;
    double b;
    int topN=50;
    double lambda=0.01;
    double threshHold=0.02;
    int semanticAddition=1;
    //</editor-fold>

    //<editor-fold desc="constructor">

    /**
     * constructor
     * @param indexer
     */
    public Ranker(Indexer indexer) {
        FTable = new Hashtable<>();
        this.indexer = indexer;
        do_entities=false;
        do_semantic=false;
        query = new ArrayList<>();
        records = new ArrayList<>();
        docs = new HashSet<>();
        lambda = 0;
        threshHold=0.004275;//default value
        k1=2;//default value from [1.2,2.0]
        b=0.5;//default value
    }
    //</editor-fold>

    //<editor-fold desc="rankBM25() - rank with BM25 using Hashtables">

    /**
     *
     * @param FTable - an Hashtable with key - term (from query), value - Hashtable with key - doc ,value - f of the term in the doc
     * @return a list of the top 50 ranked docs for the query
     */
    public ArrayList<String> rankBM25(Hashtable<String,Hashtable<String,Integer>> FTable)
    {
        this.FTable= FTable;
        ArrayList<Pair<String,Double>>  docAndScore = new ArrayList<>();
        if(FTable==null||FTable.size()==0)
            return null;
        Set<String> terms = FTable.keySet();
        HashSet<String> docs=new HashSet<String>();
        for (String term:terms) {
            int score=0;
            for (String doc:FTable.get(term).keySet()) {
                docs.add(doc);
            }
        }
        for (String doc:docs) {
            double score=0.0;
            for (String term:terms) {
                score+=score(term,doc);
            }
            docAndScore.add(new Pair<String,Double>(doc,score));
        }
        docAndScore.sort((o1, o2) -> o2.getValue().compareTo(o1.getValue()));
        ArrayList<String> result = new ArrayList<>();
        //all of the docs
        for (int i = 0; i < topN&&i<docAndScore.size(); i++) {
            result.add(docAndScore.get(i).getKey()+" "+docAndScore.get(i).getValue());
        }
        return result;
    }
    //</editor-fold>

    //<editor-fold desc="formulas">

        //<editor-fold desc="BM25 formulas">

    /**
     * formula for score by MB25
     * @param term
     * @param doc
     * @return score value for a term in a doc
     */
        public double score(String term,String doc) {
            return IDF_BM25(term)* ((f(term,doc))*(k1+1)) /(f(term,doc)+k1*(1-b+b*(D(doc)/avgdl())));
        }

    /**
     * getter for the f of a term in a doc
     * @param term
     * @param doc
     * @return
     */
        public double f(String term,String doc) {
        try {
            return FTable.get(term).get(doc);
        }catch(Exception e)
        {
            return 0.0;
        }
    }

    /**
     *  formula for IDF by BM25
      * @param term
     * @return IDF value of a term
     */
    private double IDF_BM25(String term)
        {
            try {
                double res = ((Math.log((N() - n(term) + 0.5)) / (n(term) + 0.5)))/Math.log(2);
                return res;
            }catch(Exception e){
                return -1;
            }
        }

    /**
     * getter for the n of a term - similar to f of a term
     * @param term
     * @return
     */
    private int n(String term) {
            return df(term);
        }

    /**
     * getter for the average document length in the corpus
     * @return
     */
    private double avgdl()
        {
            return indexer.avgdl();
        }
        //</editor-fold>

        //<editor-fold desc="formulas from lecture">

        //docID <termName,[#maxf,#uniqueTerms,length]>

    /**
     * getter for the most common term in a doc
     * @param doc
     * @return
     */
        public String commonTerm(String doc) {
            if (doc == null || !indexer.getDocDictionary().containsKey(doc))
                return "";
            String term = indexer.getDocDictionary().get(doc).getKey();
            if (term == null)
                return "";
            return term;
        }

    /**
     * formula for a term's weight in a doc using D normalization
     * @param term
     * @param doc
     * @return
     */
        public double w_D(String term,String doc){
            return tfD(term,doc)*idf(term);
        }
    /**
     * formula for a term's weight in a doc using maxF normalization
     * @param term
     * @param doc
     * @return
     */
        public double w_maxf(String term,String doc){
            return tfmaxf(term,doc)*idf(term);
        }
    /**
     * formula for a term's tf in a doc using maxf normalization
     * @param term
     * @param doc
     * @return
     */
        private double tfmaxf(String term,String doc) {
            return f(term)/maxf(doc);
        }
    /**
     * formula for a term's tf in a doc using D normalization
     * @param term
     * @param doc
     * @return
     */
        private double tfD(String term,String doc) {
            return f(term)/D(doc);
        }
    /**
     * formula for a term's idf in the corpus
     * @param term
     * @return
     */
        private double idf(String term)
        {
            double Ndf =N()/df(term);
            return Math.log(Ndf)/Math.log(2);
        }

    /**
     * getter for the number of docs in the corpus
     * @return
     */
    private int N() {
            Set<String> terms = indexer.getDocDictionary().keySet();
            if(terms==null||terms.size()==0)
                return 0;
            return terms.size();
        }

    /**
     * getter for the maxf of the most common term in a doc
     * @param doc
     * @return
     */
    private int maxf(String doc) //number of instances of the mot common term in the doc
        {
            if(getDocData(0,doc)==0)
                return Integer.MAX_VALUE;
            return getDocData(0,doc);
        }

    /**
     * getter for the number of unique terms in a doc
     * @param doc
     * @return
     */
    private int uniqueTerms(String doc)//document length
        {
            if(getDocData(1,doc)==0)
                return Integer.MAX_VALUE;
            return getDocData(1,doc);
        }

    /**
     * getter for the length of a doc
     * @param doc
     * @return
     */
    private int D(String doc)//document length
        {
            if(getDocData(2,doc)==0)
                return Integer.MAX_VALUE;
            return getDocData(2,doc);
        }

    /**
     * getter for the df of a term
     * @param term
     * @return
     */
    private int df(String term)//number of docs containing the term
        {
            return getTermData(0,term);
        }

    /**
     * getter for the total f of a term in the corpus
     * @param term
     * @return
     */
    private int f(String term)//number of all instances
        {
            return getTermData(1,term);
        }
        //</editor-fold>

    //</editor-fold>

    //<editor-fold desc="setters">
    public void setEntities(boolean do_entities) {
        this.do_entities = do_entities;
    }

    public void setSemantic(boolean do_semantic) {
        this.do_semantic = do_semantic;
    }
    //</editor-fold>

    //<editor-fold desc="getData">

    /**
     * helper function for getter a certain detail of a doc
     * @param index
     * @param doc
     * @return
     */
    private int getDocData(int index,String doc)
    {
        if(doc==null||!indexer.getDocDictionary().containsKey(doc))
            return 0;
        int[] docData = indexer.getDocDictionary().get(doc).getValue();
        if(docData==null)
            return 0;
        return docData[index];
    }

    /**
     * helper function for getting a certain term detail
     * @param index
     * @param term
     * @return
     */
    private int getTermData(int index,String term)
    {
        if(term==null)
            return 0;
        int[] termData = indexer.getDictionary().get(term);
        if(termData==null)
            return 0;
        return termData[index];
    }

    /**
     * ranks the entities in the doc using tf-idf value
     * @param doc
     * @param entities
     * @return list of top 5 ranked entities
     */
    public List<String> rankEntitiesInDoc(String doc, List<String> entities) {
        ArrayList<Pair<Double,String>> entityAndRank = new ArrayList<>();
        for (String entity:entities) {
            entityAndRank.add(new Pair<Double, String>(w_maxf(entity,doc),entity));
        }
        entityAndRank.sort(new Comparator<Pair<Double, String>>() {
            @Override
            public int compare(Pair<Double, String> o1, Pair<Double, String> o2) {
                return o2.getKey().compareTo(o1.getKey());
            }
        });
        if(entities.size()<5)
            return entities;
        return entities.subList(0,5);
    }
    //</editor-fold>

}
