package sample;

import javafx.util.Pair;
import java.io.*;
import java.lang.reflect.Array;
import java.nio.Buffer;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class Indexer {

    private String currentDocID;
    private ArrayList<Pair<String, String>> collection;
    private ArrayList<String> keys;

    public Hashtable<String, int[]> getDictionary() {
        return Dictionary;
    }

    private Hashtable<String, int[]> Dictionary;
    private ArrayList<String> postingRecords;
    private Hashtable<String, ArrayList<String>> acPostString;
    private Hashtable<String,Set<String>> docEntities;
    private boolean withStemming = false;
    private double avgdl=0;

    public String getStemmingPath() {
        return stemmingPath;
    }

    private String stemmingPath = "";


    //docID <termName <#maxf,#uniqueTerms,length>>
    private Hashtable<String, Pair<String, int[]>> docDictionary;
    int docCounter = 0;
    String CorpusPath = "";
    String PostingPath = "";
    Hashtable<String, Integer> fTable;

    //<editor-fold desc="default constructor">
    public Indexer() {
        fTable = new Hashtable<String, Integer>();
        collection = new ArrayList<>();
        keys = new ArrayList<>();
        Dictionary = new Hashtable<>();
        docDictionary = new Hashtable<String, Pair<String, int[]>>();
        currentDocID = "";
        postingRecords = new ArrayList<>();
        docEntities = new Hashtable<>();
        acPostString = new Hashtable<>();
        withStemming = false;
    }
    //</editor-fold>
    //<editor-fold desc="sortCollection - sorting the collection array">

    /**
     * sorts a given ArrayList using a default comparator of the given object
     *
     * @param pairArrayList
     */
    private void sortCollection(ArrayList<Pair<String, String>> pairArrayList) {
        pairArrayList.sort((o1, o2) -> o1.getKey().compareTo(o2.getKey()));
    }
    //</editor-fold>
    //<editor-fold desc="addToDocDictionary - adding a document to DocDictionary">

    /**
     * @param DocId           the ID of a given document
     * @param mostCommonTerm  the most common term in the document
     * @param maxf            the number of instances of the most common term in the document
     * @param uniqueTermCount the number of the unique terms in the document
     * @param length          the length of the document
     */
    public void addToDocDictionary(String DocId, String mostCommonTerm,
                                   int maxf, int uniqueTermCount, int length) {
        int[] docData = new int[3];
        docData[0] = maxf;
        docData[1] = uniqueTermCount;
        docData[2] = length;
        Pair<String, int[]> value = new Pair<String, int[]>(mostCommonTerm, docData);
        docDictionary.put(DocId, value);
    }
    // </editor-fold>
    //<editor-fold desc="createFile - creates a File object for Posting files">

    /**
     * wrapper function for createFile(String) using a sample.Term.Text field
     *
     * @param term a sample.Term object to create a posting file for
     * @return File object of the matching Posting File for the term
     */
    public File createFile(Term term) {
        return createFile(term.getText().toLowerCase());
    }

    /**
     * @param term a term to create a Posting File for
     * @return File object of the matching Posting File for the term
     */
    public File createFile(String term) {
        if (term.length() < 2)
            return null;
        File newfile = null;
        FileWriter fw;
        String path = termToFilePath(term, PostingPath + stemmingPath + "\\");
        boolean result = false;
        try {
            newfile = new File(path);
            result = newfile.createNewFile();
        } catch (Exception e) {
            System.out.println("couldn't create " + path);
        }
        return newfile;
    }
    //</editor-fold>
    //<editor-fold desc="termToFilePath - finds the path for the matching posting file">

    /**
     * @param term           term to get the posting file path for
     * @param directory_path path of posting directory
     * @return an absolute path for the matching posting file
     */
    public String termToFilePath(String term, String directory_path) {
        String path;
        if (term.length() > 1)
            path = directory_path + "\\" + term.toLowerCase().substring(0, 2);
        else
            return null;
        if (term.contains("&"))
            return "\\_&";

        if (path.startsWith(" "))
            path = directory_path + "\\" + term.toLowerCase().substring(1, 3);
        if (!(!path.contains("0") && !path.contains("1") && !path.contains("2")
                && !path.contains("3") && !path.contains("4") && !path.contains("5")
                && !path.contains("6") && !path.contains("7") && !path.contains("8")
                && !path.contains("9"))) {
            path = directory_path + "\\_num";
            path = path.replace('"' + "", "");
            return path;
        }
        path = path.replace('"' + "", "");
        if (term.contains("%"))
            return "\\_%";
        return path;
    }
    //</editor-fold>
    //<editor-fold desc="clearPostingDirectory - clear the posting files">

    /**
     * delete all the posting files from Posting directory
     *
     * @return false if all files was not deleted
     */
    public boolean clearPostingDirectory() {
        File file = new File(PostingPath + stemmingPath + "\\");
        file.list((current, name) -> new File(current, name).isDirectory());
        File[] files = file.listFiles();
        if (files == null || files.length == 0)
            return false;
        boolean isPostFile = false;
        for (File fileToDelete : files) {
            if ((fileToDelete.getName().toLowerCase().equals("ir_project")) ||(fileToDelete.getName().toLowerCase().equals("stop_words.txt"))|| fileToDelete.isDirectory() || !fileToDelete.delete()) {
                System.out.println("failed to delete " + fileToDelete.getName());
            } else {
                System.out.println(fileToDelete.getName() + " deleted successfully");
            }
        }
        return true;
    }

    //</editor-fold>
    // <editor-fold desc="getter and setter for CorpusPath">
    public String getCorpusPath() {
        return CorpusPath;
    }

    public void setCorpusPath(String CorpusPath) {
        this.CorpusPath = CorpusPath;
    }
    // </editor-fold>
    // <editor-fold desc="dictionaryToString - creates a String with readable data from the Dictionary">

    /**
     * creates string with all the data in Dictionary
     *
     * @return String object with readable data from the Dictionary object
     */
    public ArrayList<String> dictionaryToString() {
        ArrayList<String> dictionaryList = new ArrayList<>();
        if (Dictionary == null || Dictionary.isEmpty()) {
            dictionaryList.add("no dictionary information available");
            return dictionaryList;
        }
        Iterator<String> terms = null;
        try {
            terms = (Iterator<String>) Dictionary.keys();
        } catch (Exception e) {
            System.out.println("empty Dictionary " + e);
            return null;
        }
        String term = "";
        while (terms.hasNext()) {
            term = terms.next();
            dictionaryList.add(term + ";" + Dictionary.get(term)[0] + "," + Dictionary.get(term)[1] +"," + Dictionary.get(term)[2]+"\n");
        }
        return dictionaryList;
    }
    // </editor-fold>

    // <editor-fold desc="saveDictionary - saves the Dictionary to the memory">

    /**
     * saves all the data from Dictionary to a File
     */
    public void saveDictionary() {
        System.out.println("saving dictionary...");
        if (Dictionary == null || Dictionary.isEmpty())
            System.out.println("no dictionary information available");

        BufferedWriter dictionaryWriter = null;
        Iterator<String> iter = null;
        try {
            dictionaryWriter = new BufferedWriter(new FileWriter(new File(PostingPath + stemmingPath + "\\dictionary")));
            iter = (Iterator<String>) Dictionary.keys();
        } catch (Exception e) {
            System.out.println("empty Dictionary " + e);
            return;
        }
        String term = "";
        try {
            while (iter.hasNext()) {
                term = iter.next();
                term = term + ";" + Dictionary.get(term)[0] + "," + Dictionary.get(term)[1] +","+Dictionary.get(term)[2]+ "\n";
                dictionaryWriter.write(term);
            }
        } catch (Exception e) {
            System.out.println("could not save dictionary");
        }
        try {
            dictionaryWriter.close();
        } catch (Exception e) {
            System.out.println("save dictionary error");
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////////////
        BufferedWriter docWriter=null;
        try {
            docWriter = new BufferedWriter(new FileWriter(new File(PostingPath + stemmingPath + "\\document_dictionary"),true));
            iter = (Iterator<String>) docDictionary.keys();
        } catch (Exception e) {
            System.out.println("empty docDictionary " + e);
            return;
        }
        String doc = "";
        try {

            while (iter.hasNext()) {
                doc = iter.next();
                Pair<String, int[]> docData = docDictionary.get(doc);
                int[] moreDocData = docData.getValue();
                doc = doc + ";" + docData.getKey() + "," + moreDocData[0] + "," + moreDocData[1] + "," + moreDocData[2] + "\n";
                docWriter.append(doc);
            }
        } catch (Exception e) {
        }
        try {
            docWriter.close();
        } catch (Exception e) {
            System.out.println("3");
        }
        System.out.println("document dictionary saved successfully");
        ////////////////////////////////////////
        BufferedWriter entityWriter = null;
        try {
            entityWriter = new BufferedWriter(new FileWriter(new File(PostingPath + stemmingPath + "\\document_entities"),true));
            iter = (Iterator<String>) docEntities.keys();
        } catch (Exception e) {
            System.out.println("empty docEntities " + e);
            return;
        }
        String docID = "";
        try {

            while (iter.hasNext()) {
                docID = iter.next();
                Set<String> entitySet = docEntities.get(docID);
                StringBuilder entity_list = new StringBuilder();
                entity_list.append(docID+";");
                for (String entity:entitySet) {
                    entity_list.append(entity+",");
                }
                entity_list.append("\n");
                entityWriter.append(entity_list.toString());
            }
        } catch (Exception e) {
        }
        try {
            entityWriter.close();
        } catch (Exception e) {
            System.out.println("could not create document entities");
        }
        System.out.println("document entities saved successfully");
    }


    // </editor-fold>
    // <editor-fold desc="loadDicitonary - loads the Dictionaries from the memory">

    /**
     * loads data to the Dictionary from a the memory
     */
    public void loadDicitonary() {
        Dictionary.clear();
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(
                            new File(PostingPath + stemmingPath + "\\dictionary")));
            String line;
            String key = "";
            String[] arrDocs;
            while ((line = br.readLine()) != null) {
                ArrayList<String> docs = new ArrayList<>();
                String[] dfAndPointerString = new String[2];
                int[] dfAndPointerInteger = new int[3];
                String[] termAndArray = line.split(";");
                if (termAndArray.length < 2)
                    break;
                key = termAndArray[0];
                dfAndPointerString = termAndArray[1].split(",");
                dfAndPointerInteger[0] = Integer.parseInt(dfAndPointerString[0]);
                dfAndPointerInteger[1] = Integer.parseInt(dfAndPointerString[1]);
                dfAndPointerInteger[2] = Integer.parseInt(dfAndPointerString[2]);
                Dictionary.put(key, dfAndPointerInteger);
            }
            br.close();
        } catch (Exception e) {
            System.out.println("could'nt load dictionary from memory");
        }
        docDictionary.clear();
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(
                            new File(PostingPath + stemmingPath + "\\document_dictionary")));
            String line;
            String docID = "";
            String mostCommonTerm = "";
            int maxf = 0;
            int termCountInDoc = 0;
            int docLength = 0;
            String[] arrDocsInfo;
            while ((line = br.readLine()) != null) {
                String[] docAndArray = line.split(";");
                if (docAndArray.length < 2)
                    break;
                docID = docAndArray[0];
                arrDocsInfo = docAndArray[1].split(",");
                int size = arrDocsInfo.length - 1;
                if (arrDocsInfo[size].contains("!"))
                    docLength = Integer.parseInt(arrDocsInfo[size].substring(0, arrDocsInfo[3].length() - 1));
                else
                    docLength = Integer.parseInt(arrDocsInfo[size]);
                size--;
                termCountInDoc = Integer.parseInt(arrDocsInfo[size]);
                size--;
                maxf = Integer.parseInt(arrDocsInfo[size]);
                size--;
                if (size != 0) {
                    while (size != 0) {
                        mostCommonTerm += arrDocsInfo[size];
                        size--;
                    }
                }else {
                    mostCommonTerm = arrDocsInfo[0];
                }

                addToDocDictionary(docID, mostCommonTerm, maxf, termCountInDoc, docLength);
            }
            br.close();
        } catch (Exception e) {
            System.out.println("could'nt load document dictionary from memory \n"+e);
        }
        docEntities.clear();
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader(
                            new File(PostingPath + stemmingPath + "\\document_entities")));
            String docID = "";
            String[] docAndList=null;
            String line="";
            String[] entities=null;
            HashSet<String> entitySet=new HashSet<>();
            while ((line = br.readLine()) != null) {
                docAndList = line.split(";");
                docID=docAndList[0];
                entitySet=new HashSet<>();
                if(docAndList.length>1) {
                    entities = docAndList[1].split(",");
                    for (int i = 0; i < entities.length &&i<15; i++) {
                        if (entities[i].length() != 0)
                            entitySet.add(entities[i]);
                    }
                }
                docEntities.put(docID,entitySet);
//                if(docEntities.keySet().size()%20000==0)
//                    System.out.println(docEntities.keySet().size());
            }
            br.close();
        } catch (Exception e) {
            System.out.println("could'nt load document entities from memory \n"+e);
        }
        System.out.println();

    }
    // </editor-fold>
    // <editor-fold desc="documentDictionryToString - creates a String with readable data from the DocDictionary">

    /**
     * creates string with all the data in DocDictionary
     *
     * @return
     */
    public String documentDictionryToString() {
        if (docDictionary == null || docDictionary.isEmpty())
            return "no document dictionary information available";
        String dictionaryList = "";
        ArrayList<String> documentsKeys = new ArrayList<>();
        Iterator<String> iter = null;
        try {
            iter = (Iterator<String>) docDictionary.keys();
        } catch (Exception e) {
            return null;
        }
        while (iter.hasNext())
            documentsKeys.add(iter.next());
        for (String key : documentsKeys) {
            Pair<String, int[]> docData = docDictionary.get(key);
            int[] moreDocData = docData.getValue();
            if (moreDocData != null && moreDocData.length >= 2) {
                dictionaryList +=
                        key + ";" + docData.getKey() + "," + moreDocData[0] + "," + moreDocData[1] + "," + moreDocData[2] + "\n";
            }
        }
        return dictionaryList;
    }
    //</editor-fold>
    //<editor-fold desc="addToCollection - add a document to the index Collection">

    /**
     * wrapper function for addToCollection(sample.DocumentObj)
     *
     * @param docs arrayList of document for indexing
     */
    public void addToCollection(ArrayList<DocumentObj> docs) {
        for (DocumentObj doc : docs) {
            addToCollection(doc);
        }
    }

    /**
     * add a sample.DocumentObj for indexing
     *
     * @param document
     */
    public void addToCollection(DocumentObj document) {
        currentDocID = document.getID();
        HashSet<String> entities = new HashSet<>();
        docEntities.put(currentDocID,entities);
        collection.clear();//start with an empty collection
        if (document == null) {
            System.out.println("null sample.sample.DocumentObj in indexDocument in class sample.sample.Indexer");
            return;
        }
        Term term = document.getTerms();
        if (term == null) {
            System.out.println("null term in addTermToCollection");
            return;
        }
        int D = 0;//document length
        while (term != null)//add the terms to the collection with their docID
        {
            collection.add(new Pair<String, String>(term.getText(), currentDocID));
            term = term.getNext();
            D++;
        }
        fTable.clear();
        int maxf = 0;
        int uniqueTermCount = 0;
        String maxTerm = "";
        String termInDoc;
        for (int i = 0; i < collection.size(); i++) {
            termInDoc = collection.get(i).getKey();
            if(termInDoc.charAt(0)>='A'&&termInDoc.charAt(0)<='Z')
                termInDoc = termInDoc.toUpperCase();
            else
                termInDoc = termInDoc.toLowerCase();
            if (fTable.containsKey(termInDoc)) {
                Integer f = fTable.get(termInDoc);
                f++;
                fTable.remove(termInDoc);
                fTable.put(termInDoc, f);
                if (f > maxf) {
                    maxf = f;
                    maxTerm = termInDoc;
                }
            } else {
                if(termInDoc.charAt(0)>='A'&&termInDoc.charAt(0)<='Z')
                    docEntities.get(currentDocID).add(termInDoc);

                fTable.put(termInDoc, 1);
                uniqueTermCount++;
            }
        }
        addToDocDictionary(currentDocID, maxTerm, maxf, uniqueTermCount, D);
    }
    //</editor-fold>
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // <editor-fold desc="setDictionery - updates the Dictionary with data from Documents">

    /**
     * updates the Dictionary with data from Documents
     */
    public void setDictionery() {
        int currentTermIndex = 0;
        if (Dictionary == null)
            Dictionary = new Hashtable<>();
        Iterator<String> terms = null;
        try {
            terms = (Iterator<String>) fTable.keys();
        } catch (Exception e) {
            System.out.println(e + "in setDictionary");
            return;
        }

        while (terms.hasNext()) {
            String term = terms.next();
            int[] dfAndPointer;
            dfAndPointer = Dictionary.get(term.toLowerCase());
            if (dfAndPointer == null)//the term is not in the dictionary in capital form
                dfAndPointer = Dictionary.get(term.toUpperCase());

            if (Dictionary.containsKey(term.toLowerCase())) {
                dfAndPointer[0] = dfAndPointer[0] + 1;

                if(fTable.get(term.toLowerCase())==null) {
                    dfAndPointer[1] = dfAndPointer[1]+fTable.get(term.toUpperCase());
                }
                else
                    dfAndPointer[1] = dfAndPointer[1]+fTable.get(term.toLowerCase());

                Dictionary.put(term.toLowerCase(), dfAndPointer);

            } else if (Dictionary.containsKey(term.toUpperCase())) {
                if ('a' <= term.charAt(0) && term.charAt(0) <= 'z') {
                    dfAndPointer[0] = dfAndPointer[0] + 1;
                    int f=0;
                    if(fTable.get(term.toUpperCase())==null)
                        f=fTable.get(term.toLowerCase());
                    else
                        f=fTable.get(term.toUpperCase());
                    dfAndPointer[1] = dfAndPointer[1]+f;
                    Dictionary.remove(term.toUpperCase());
                    Dictionary.put(term.toLowerCase(), dfAndPointer);
                } else {
                    dfAndPointer[0] = dfAndPointer[0] + 1;
                    String key="";
                    if(fTable.get(term.toUpperCase())==null) {
                        dfAndPointer[1] = dfAndPointer[1]+fTable.get(term.toLowerCase());
                    }
                    else
                        dfAndPointer[1] = dfAndPointer[1]+fTable.get(term.toUpperCase());
                    Dictionary.put(term.toUpperCase(), dfAndPointer);
                }
            } else {
                int[] arrayWithFirstValue = new int[3];
                int f=0;
                arrayWithFirstValue[0] = 1;
                if(fTable.get(term.toLowerCase())!=null) {
                    f = fTable.get(term.toLowerCase());
                }else if(fTable.get(term.toUpperCase())!=null){
                    f = fTable.get(term.toUpperCase());
                }else
                    f= fTable.get(term);
                if (term.length() != 0 && 'a' <= term.charAt(0) && term.charAt(0) <= 'z') {
                    term = term.toLowerCase();
                } else {
                    term = term.toUpperCase();
                }
                arrayWithFirstValue[1]=f;
                arrayWithFirstValue[2] = 1;
                Dictionary.put(term, arrayWithFirstValue);
            }
        }
    }
    // </editor-fold>
    // <editor-fold desc="stackRecords - accumulates String of data for later writing to a Posting File">

    /**
     * accumulates String of data for later writing to a Posting File
     */
    public void stackRecords() {

        Iterator<String> keysIterator = null;
        try {
            keysIterator = (Iterator<String>) fTable.keys();
        } catch (Exception e) {
            System.out.println("empty fTable " + e);
            return;
        }
        while (keysIterator.hasNext()) {
            String term = keysIterator.next();
            String line = term + ";" + currentDocID + "," + fTable.get(term) + "\n";
            String prefix = termToFilePath(line, "");
            prefix = prefix.substring(1);
            if (acPostString.containsKey(prefix)) {
                ArrayList<String> newLine = acPostString.get(prefix);
                newLine.add(line);
                acPostString.put(prefix, newLine);
            } else {
                ArrayList<String> newLine = new ArrayList<>();
                newLine.add(line);
                acPostString.put(prefix, newLine);
            }
        }
        fTable.clear();
        docCounter++;
        if (docCounter >= 5000) {
            docCounter = 0;
            Iterator<String> iter = null;
            BufferedWriter postFileout;
            try {
                postFileout = new BufferedWriter(new FileWriter(new File(PostingPath + stemmingPath + "\\document_dictionary"),true));
                iter = (Iterator<String>) docDictionary.keys();
            } catch (Exception e) {
                System.out.println("empty docDictionary " + e);
                return;
            }
            String doc = "";
            try {

                while (iter.hasNext()) {
                    doc = iter.next();
                    Pair<String, int[]> docData = docDictionary.get(doc);
                    int[] moreDocData = docData.getValue();
                    doc = doc + ";" + docData.getKey() + "," + moreDocData[0] + "," + moreDocData[1] + "," + moreDocData[2] + "\n";
                    postFileout.append(doc);
                }
            } catch (Exception e) {
            }
            try {
                postFileout.close();
            } catch (Exception e) {
                System.out.println("3");
            }
            System.out.println("document dictionary saved successfully");
            docDictionary.clear();
            iter=null;
            BufferedWriter EntityFile = null;
            try {
                EntityFile = new BufferedWriter(new FileWriter(new File(PostingPath + stemmingPath + "\\document_entities"),true));
                iter = (Iterator<String>) docEntities.keys();
            } catch (Exception e) {
                System.out.println("empty docEntities " + e);
            }
            String docID = "";
            try {

                while (iter!=null&&iter.hasNext()) {
                    docID = iter.next();
                    Set<String> entitySet = docEntities.get(docID);
                    StringBuilder entity_list = new StringBuilder();
                    entity_list.append(docID+";");
                    for (String entity:entitySet) {
                        entity_list.append(entity+",");
                    }
                    entity_list.append("\n");
                    EntityFile.append(entity_list.toString());
                }
            } catch (Exception e) {
            }
            try {
                EntityFile.close();
            } catch (Exception e) {
                System.out.println("could not create document entities");
            }
            System.out.println("document entities saved successfully");
            docEntities.clear();

            createPosting();
        }
    }
    //</editor-fold>
    //////////////////////////////////////////////////////////////////////////////////
    // <editor-fold desc="createPosting - creates and/or writes posting records to Posting File">

    /**
     * creates/updates Posting Files
     */
    public void createPosting() {

        Iterator<String> keysIterator = null;
        try {
            keysIterator = (Iterator<String>) acPostString.keys();
        } catch (Exception e) {
            System.out.println("could not create posting");
            return;
        }
        String key = "";
        ArrayList<String> records = new ArrayList<>();
        while (keysIterator.hasNext()) {
            key = keysIterator.next();
            records = acPostString.get(key);
            File postFile = new File(key);
            if (postFile == null)
                postFile = createFile(key);
            Writer output;
            try {
                output = new BufferedWriter(new FileWriter(PostingPath + stemmingPath + "\\" + postFile, true));  //clears file every time
                for (String line:records) {
                    output.write(line);
                }
                output.close();
            } catch (Exception e) {
                System.out.println(e + " could'nt create/update file " + key);
            }
        }
        System.out.println("posting created/updated successfully");
        acPostString.clear();
    }
    //</editor-fold>
    //<editor-fold desc="sortPosting - sorts all the records in all the posting files">

    /**
     * sorts all the Posting Files
     */
    public void sortPosting() {
        File file = new File(PostingPath + stemmingPath + "\\");
        file.list((current, name) -> new File(current, name).isFile());
        File[] files = file.listFiles();
        for (File fileToSort : files) {
            if (!(fileToSort.getName().equals("dictionary")) && !(fileToSort.getName().equals("document_dictionary"))&& !(fileToSort.getName().equals("document_entities"))) {
                ArrayList<String> lines = new ArrayList<>();
                try {
                    BufferedReader br = new BufferedReader(new FileReader(fileToSort));
                    String line = "";
                    while ((line = br.readLine()) != null) {
                        lines.add(line);
                    }
                    PrintWriter wr = new PrintWriter(fileToSort);
                    wr.print("");
                    wr.close();
                    br.close();
                } catch (Exception e) {
                    System.out.println(e + " could'nt sort file " + file.getName());
                }
                lines.sort((o1, o2) -> o1.compareTo(o2));
                file = new File(PostingPath + stemmingPath + "\\");
                file.list((current, name) -> new File(current, name).isFile());
                files = file.listFiles();
                try {
                    fileToSort.createNewFile();
                    BufferedWriter output = new BufferedWriter(new FileWriter(fileToSort, true));  //clears file every time
                    String line = "";
                    String lastTerm = "";
                    String term = "";
                    int pointer = 1;//______________________________first line is line 1
                    for (int i = 0; i < lines.size(); i++) {
                        line = lines.get(i);
                        if(!line.equals("")) {
                            line = line + "\n";
                            if(true)/////////////////////////////////////////??????????????
                                output.write(line.split(";")[1]);
                            else
                                output.write(line);
                            term = line.split(";")[0];
                            if (!term.equals(lastTerm)) {
                                if (Dictionary.get(term.toLowerCase()) == null) {
                                    int[] arr = Dictionary.get(term.toUpperCase());
                                    if (arr == null) {
                                    }
                                    //System.out.println("++++++++++++++++++++++++++++"+term+" "+fileToSort);
                                    else {
                                        arr[2] = pointer;
                                    }
                                } else
                                    Dictionary.get(term.toLowerCase())[2] = pointer;
                                lastTerm = term;
                            }
                            pointer++;
                        }
                    }
                    output.flush();
                    output.close();
                } catch (Exception e) {
                    System.out.println("class index could not sort " + fileToSort.getName() + " in sortPosting");
                }
            }
        }
    }//sortPosting

    //</editor-fold>
    //<editor-fold desc="setWithStemming - switch the path for stemming folder">
    public void setWithStemming(boolean withStemming) {
        this.withStemming = withStemming;
        if (withStemming) {
            stemmingPath = "\\withStemming";
        } else {
            stemmingPath = "";
        }
    }
    //</editor-fold>
    //<editor-fold desc="clear - clears all the data structures in the indexer">

    /**
     * clears all data from the data structures in the indexer
     */
    public void clear() {
        fTable.clear();
        collection.clear();
        keys.clear();
        docDictionary.clear();
        postingRecords.clear();
        acPostString.clear();
        docEntities.clear();
    }

    public void clearDictionary() {
        Dictionary.clear();
    }

    //</editor-fold>
    //<editor-fold desc="posting path getter">
    public String getPostingPath() {
        return PostingPath;
    }

    // </editor-fold>
    //<editor-fold desc="posting path getter">
    public void setPostingPath(String postingPath) {
        PostingPath = postingPath;
    }
    // </editor-fold>

    //<editor-fold desc="Part B additions">
    public Hashtable<String, Pair<String, int[]>> getDocDictionary() {
        return docDictionary;
    }

    /**
     * getter for the average length of a doc
     * @return
     */
    public double avgdl()
    {
        if(avgdl!=0)
            return avgdl;
        //calculate avg doc length
        int sumLength=0;
        for (String doc:docDictionary.keySet()) {
            sumLength += docDictionary.get(doc).getValue()[2];
        }
        avgdl = sumLength/docDictionary.keySet().size();
        return avgdl;
    }

    /**
     * getter for the entities in a given doc
     * @param doc
     * @return
     */
    public ArrayList<String> getDocEntities(String doc)
    {
        ArrayList<String> entities = new ArrayList<>();
        Set<String> entitiesInDoc = docEntities.get(doc);
        if(entitiesInDoc!=null) {
            for (String entity : entitiesInDoc) {
                if (Dictionary.containsKey(entity)) {
                    entities.add(entity);
                }
            }
        }
        else
            System.out.println();
        return entities;
    }
    public Hashtable<String, Set<String>> getDocEntities() {
        return docEntities;
    }
    //</editor-fold>


}//class
