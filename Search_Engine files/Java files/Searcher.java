package sample;

import com.medallia.word2vec.Word2VecModel;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Array;
import java.util.*;


public class Searcher {

    //<editor-fold desc="vars">
    Indexer indexer;
    Parse parser;
    Ranker ranker;
    int semanticAddition;
    double distance;
    SimpleBooleanProperty doEntities;
    SimpleBooleanProperty doSemantic;
    ArrayList<String> paths = new ArrayList<>();
    //</editor-fold>
    /**
     * constractor
     * @param indexer- pointer to the indexer object
     * @param parser- pointer to the indexer object
     */
    public Searcher(Indexer indexer, Parse parser)
    {
        doEntities=new SimpleBooleanProperty(false);
        doSemantic=new SimpleBooleanProperty(false);
        this.indexer = indexer;
        this.parser = parser;
        ranker = new Ranker(indexer);
        //if(parser.stopWordsSet==null||parser.stopWordsSet.size()==0)
            //parser.loadStopWords("src\\05_stop_words.txt");
    }

    /**1
     * the main method of the Searcher class
     * find for a querie or a list of queries the top 50 documents
     * that are relevance for them from the corpus.
     * @param queries - list of queries
     * @return Hashtable - the input queries and the relevance documents for them.
     */
    public Hashtable<String, ArrayList<String>> processQueries(ArrayList<String> queries)
    {
        Hashtable<String,ArrayList<String>> queryAndDocs = new Hashtable<>();
        for (int i = 0; i < queries.size(); i++) {
            String key = queries.get(i);//the query itself
            if(queries.get(i)!=null&&queries.get(i).length()!=0) {
                ArrayList<String> parsedQuery = ParseQuery(key);//turn query to a list of terms
                ///switch the lower/upper case
                for (int j = 0; j < parsedQuery.size(); j++) {
                    if(!indexer.getDictionary().containsKey(parsedQuery.get(j)))
                    {
                        if(parsedQuery.get(j).charAt(0)>='A'&&parsedQuery.get(j).charAt(0)<='Z')
                            parsedQuery.set(j,parsedQuery.get(j).toLowerCase());
                        else
                            parsedQuery.set(j,parsedQuery.get(j).toUpperCase());

                    }
                }
                ///////
                ArrayList<String> semanticQuery = new ArrayList<>();
                if(doSemantic.getValue())
                {
                    boolean finished=true;
                    for (String term:parsedQuery) {
                        try {
                            Word2VecModel model = Word2VecModel.fromTextFile(new File("src\\word2vec.c.output.model.txt"));
                            com.medallia.word2vec.Searcher semancticSearcher = model.forSearch();
                            List<com.medallia.word2vec.Searcher.Match> matches = semancticSearcher.getMatches(term.toLowerCase(), 3);
                            for (com.medallia.word2vec.Searcher.Match match : matches) {
                                if (match.distance() > 0.97) {
                                    semanticQuery.add(match.toString());
                                }
                            }
                        } catch (Exception e) {
                        }
                    }
                    if(finished) {
                        for (String term : parsedQuery) {
                            if (!semanticQuery.contains(term))
                                semanticQuery.add(term);
                        }
                        parsedQuery = semanticQuery;
                    }
                }
                //////
                Hashtable<String,Hashtable<String, Integer>> ftable =getFTableForQuery(parsedQuery);//ftable
                ArrayList<String> value = ranker.rankBM25(ftable);//list of top 50 docs from the table

                if(doEntities.getValue()&&value!=null)
                {
                    List<String> entities;
                    for (int j = 0; j <value.size() ; j++) {

                        String doc = value.get(j).split(" ")[0];
                        entities = indexer.getDocEntities(doc);
                        entities = ranker.rankEntitiesInDoc(doc,entities);
                        StringBuilder sb = new StringBuilder();
                        sb.append(value.get(j));
                        for (String entity:entities) {
                            sb.append(","+entity);
                        }
                        value.set(j,sb.toString());
                    }
                }
                if(key!=null&&value!=null)
                    queryAndDocs.put(key,value);
            }
        }
        System.out.println("done retrieval");
        return queryAndDocs;
    }

    /**1.1
     * the method get String of a Query, use methods from the "Parse" class
     * and return a list of parsed Terms.
     */
    public ArrayList<String> ParseQuery(String newQuery){
        Term termFromDoc=null;
        ArrayList<String> parseQuery = new ArrayList<String>();
        ArrayList<String> stringFromText = new ArrayList<String>();
        ArrayList<Character> Delimiters = new ArrayList<Character>();
        String currString="";
        Delimiters= parser.getParseDelimiters();
        stringFromText = parser.splitText(newQuery,Delimiters);
        if(stringFromText==null)
            return null;
        termFromDoc = parser.stringToTerm(stringFromText);
        Term currentTerm = termFromDoc;
        parser.ParseTermList(currentTerm);
        currentTerm= parser.removeStopWords(currentTerm);
        if(parser.stemmer.isEnable())
            parser.stemmer.stemTerm(currentTerm);
        ///////////////////////////////////////////////////////
        currString=currentTerm.getText();
        parseQuery.add(currString);
        while(currentTerm!=null){
            currentTerm = currentTerm.getNext();
            if(currentTerm!=null){
                currString= currentTerm.getText();
                parseQuery.add(currString);
            }
        }
        return parseQuery;
    }

    /**
     * 1.2
     * create hash table were the keys are the queries
     * and the value is a hash table contain all the terms from the query and the frequency
     */
    ////////////////  query,          < term  ,frequency>
    public Hashtable<String,Hashtable<String,Integer>> getFTableForQuery(ArrayList<String> query)
    {
        if(query==null||query.size()==0)
            return null;
        Hashtable<String,Hashtable<String,Integer>> fTable = new Hashtable<>();
        for (String term:query) {
            Hashtable<String,Integer> h = getPostingRecords(term);
            if(!(term==null||h==null||fTable==null))
                fTable.put(term,h);
        }
        return fTable;
    }

    /**1.2.1
     * function will get all the records of a term from its matching posting file
     * @param term
     * @return Hashtable with key - doc and value - f of the term in the doc
     */
    public Hashtable<String,Integer> getPostingRecords(String term)
    {
        Hashtable<String,Integer> postingRecords = new Hashtable<>();
        if(term==null||term.length()==0||indexer==null)
            return null;
        if(indexer.getDictionary()==null||indexer.getDictionary().get(term)==null)
            return null;
        int[] termInfo;
        if(indexer.getDictionary().get(term.toLowerCase())!=null)
            termInfo = indexer.getDictionary().get(term.toLowerCase());
        else
            termInfo = indexer.getDictionary().get(term.toUpperCase());

        int df = termInfo[0];
        int pointer = termInfo[2];
        String fileName = indexer.termToFilePath(term,"");
        fileName=fileName.replace("\\","");
        String postingFilePath= indexer.getPostingPath()+ indexer.getStemmingPath()+ "\\"+fileName;
        if(!paths.contains(postingFilePath))
            paths.add(postingFilePath);
        BufferedReader br=null;
        String line="";
        try {
            br = new BufferedReader(new FileReader(new File(postingFilePath)));
            for (int i = 1; i < pointer; i++) {
                line =br.readLine();
            }
            for (int i = 0; i < df; i++) {
                line = br.readLine();
                if(line!=null) {
                    String[] data = line.split(",");
                    postingRecords.put(data[0], Integer.parseInt(data[1]));
                }
                else
                    break;
                //doclist.add(term+";"+line);
            }
            br.close();
        }catch(Exception e){
            System.out.println("could not read file: "+ postingFilePath);
        }
        return postingRecords;
    }

    /**
     * set the value that state the fuctionality of the top 5 Entities for document
     * @param doEntities
     */
    public void setDoEntities(boolean doEntities) {
        this.doEntities.setValue(doEntities);
    }

    /**
     * set the value that state the fuctionality of Semantic search for the query
     */
    public void setDoSemantic(boolean doSemantic) {
        this.doSemantic.setValue(doSemantic);
    }


    public void print()
    {
        paths.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareTo(o2);
            }
        });
        for (String s:paths) {
            System.out.println(s);
        }
    }
    public void loadStopWord(String path)
    {
        parser.loadStopWords(path);
    }
}//class
