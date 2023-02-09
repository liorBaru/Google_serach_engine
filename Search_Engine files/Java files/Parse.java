package sample;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

public class Parse {

    ArrayList<Character> Delimiters;
    Hashtable<String,String> Dates;
    HashSet<Character> CapitalCharacters;
    ArrayList<String> Numbers;
    ArrayList<String> stringFromText;
    HashSet<String> stopWordsSet;
    ArrayList<String> junkChars;
    Term terms;
    ArrayList<Character> seperators;
    public Stemmer stemmer= new Stemmer();
    private Hashtable<String, String> colorTable;
    //<editor-fold desc="part A">
    /**
     *
     * the constractor of the class
     */
    public Parse()
    {
        InitilizeDictionaries();
    }
    /**
     * 2
     * the main method of the class, coordinates the entire parsing process
     */
    public DocumentObj ParseDoc(DocumentObj document) {
        Term termFromDoc=null;
        if(document==null||document.getText()==null)
            return null;
        stringFromText = splitText(document.getText(),Delimiters);
        if(stringFromText==null)
            return null;
        cleanJunkText(stringFromText);
        termFromDoc = stringToTerm(stringFromText);
        //<editor-fold desc="canceld position">
        //termFromDoc = createPositions(termFromDoc);
        //</editor-fold>
        Term currentTerm = termFromDoc;
        ParseTermList(currentTerm);
        currentTerm= removeStopWords(currentTerm);
        if(stemmer.isEnable())
            stemmer.stemTerm(currentTerm);
        document.setTerms(currentTerm);

        return document;
    }
    /**
     * 3
     * the method get called from the controller class
     * its open the contant of the "stop words file" from it path and
     * create hashTable called "stopWordsSet" from it
     * @param path
     * @return
     */
    public HashSet<String> loadStopWords(String path) {
        //path="05_stop_words.txt";
        stopWordsSet = new HashSet<>();
        BufferedReader br;
        try {
            br = new BufferedReader(
                    new FileReader(
                            path));
            String line;
            while((line = br.readLine()) != null) {
                stopWordsSet.add(line);
            }
            stopWordsSet.add(line);
            stopWordsSet.add("");
        } catch (Exception e) {
            System.out.println("Parse class could not load stop words \n"+e.toString());
        }
        return stopWordsSet;
    }
    /**
     * 2.1
     * the method get String that contaning the text of the current doc that get parse
     * and return ArrayList of the splited text into separated words.
     * @param textToSplit
     * @param delimiters
     * @return
     */
    public ArrayList<String> splitText(String textToSplit,ArrayList<Character> delimiters)
    {
        if(textToSplit==null)
            return null;
        String textToParse = textToSplit;
        int textLength = textToParse.length();
        int lastDelimIndex=0;
        int currentCharIndex=0;
        ArrayList<String> termsFromText = new ArrayList<>();
        char currentChar;
        while(currentCharIndex<textLength)
        {
            currentChar = textToParse.charAt(currentCharIndex);
            if(textToParse.charAt(currentCharIndex)==' ') {
                if(lastDelimIndex!=currentCharIndex)
                    termsFromText.add(textToParse.substring(lastDelimIndex,currentCharIndex));
                currentCharIndex++;
                lastDelimIndex=currentCharIndex;
            }else {
                for (char delim : delimiters) {
                    if (currentChar == delim) {
                        if(lastDelimIndex!=currentCharIndex)
                            termsFromText.add(textToParse.substring(lastDelimIndex, currentCharIndex));
                        if (delim != ' ')
                            termsFromText.add("" + delim);
                        lastDelimIndex = currentCharIndex+1;
                        break;
                    }
                }
                currentCharIndex++;
            }
        }
        if(lastDelimIndex!=currentCharIndex)
            termsFromText.add(textToParse.substring(lastDelimIndex));
        return termsFromText;
    }
    public ArrayList<String> splitText(String textToSplit)
    {
        return splitText(textToSplit,Delimiters);
    }
    /**
     * 2.2
     * the method traverse over the split text from "splitText"
     * and remove junk text and invalid text in the language
     */
    public void cleanJunkText(ArrayList<String> StringList){
        String currString;

        for (int i = 0; i < StringList.size(); i++)
        {
            boolean flag = false;
            for (String junk:junkChars) {
                if (StringList.get(i).contains(junk)) {
                    StringList.remove(i);
                    i--;
                    break;
                }
            }
        }//while
    }//func
    /**
     * 2.3
     * the method traverse over the split text from "splitText" , create "term"
     * object from each word and create terms chain.
     * then traverse over the terms chain and create bigger terms from others term
     * in order to create new meaning
     * @param StringList
     * @return
     */
    public Term stringToTerm(ArrayList<String> StringList) {
        if(StringList==null||StringList.size()==0)
            return null;
        Iterator<String> iter = StringList.iterator();
        if(iter==null)
            return null;
        String currString;
        Term firstTerm;
        Term prevTerm=null;
        Term currTerm=null;
        currString = iter.next();
        currTerm = new Term(currString);
        prevTerm = currTerm;
        firstTerm = currTerm;
        firstTerm.setPrev(null);
        classifyTerm(currTerm);
        //////////////////////////////////////////////////////////////////////////
        prevTerm=currTerm;
        //////////////////////////////////////////////////////////////////////////
        /////creates a chain of classified terms
        while(iter.hasNext()) {
            currTerm = new Term(iter.next());
                if ( currTerm.getText().length() >= 2 || currTerm.getText().equals("I") ||
                        currTerm.getText().toLowerCase().equals("a")|| currTerm.getText().contains("$")||
                        currTerm.getText().contains("%") || currTerm.getText().equals("1") ||currTerm.getText().equals("2")||currTerm.getText().equals("3")||
                        currTerm.getText().equals("4")|| currTerm.getText().equals("5")|| currTerm.getText().equals("6") || currTerm.getText().equals("7")||
                        currTerm.getText().equals("8")|| currTerm.getText().equals("9") || currTerm.getText().equals("0") ) {
                    classifyTerm(currTerm);
                    prevTerm.setNext(currTerm);
                    currTerm.setPrev(prevTerm);
                    prevTerm = currTerm;
                }
        }
        currTerm=firstTerm;
        //appley rules on terms
        while(currTerm!=null)
        {
            if(currTerm.getText().charAt(0)=='"') {
                mergeQuote(currTerm);
            }
            if(currTerm.isCaps()) {
                mergeCapsCase(currTerm);
            }
            if(currTerm.isNumber()) {
                mergeNumberCase(currTerm);
            }
            if(currTerm.isDate()) {
                mergeDateCase(currTerm);
            }
            trimTerm(currTerm);
            currTerm=currTerm.getNext();
        }
        return firstTerm;
    }
    /**
     * 2.3.1
     * classify each term by category of Numbers, Dates, Caps orSeparated
     * @param term
     */
    public void classifyTerm(Term term) {
        String termContent=term.getText();
        //number check
        for (String numberTerm:Numbers ) {
            if(termContent.contains("0")||termContent.contains("1")||termContent.contains("2")
                    ||termContent.contains("3")||termContent.contains("4")||termContent.contains("5")
                    ||termContent.contains("6")||termContent.contains("7")||termContent.contains("8")
                    ||termContent.contains("9"))
            {
                term.setNumber(true);
                break;
            }
            else if( termContent.toLowerCase().equals(numberTerm)) {
                term.setNumber(true);
                break;
            }
        }
        //date check
        if(termContent.length()!=0&&Dates.containsKey(termContent.toLowerCase()))
            term.setDate(true);
        //caps check
        if(termContent.length()!=0&&CapitalCharacters.contains(termContent.charAt(0)))
            term.setCaps(true);
        //seperators check
        for(char seperator:seperators) {
            if(termContent.contains(seperator+"")) {
                term.setSeparated(true);
                break;
            }
        }
    }
    /**
     * 2.3.2
     * merge terms in the terms chain that classify as Quote
     * @param currTerm
     * @return
     */
    private Term mergeQuote(Term currTerm) {
        if(currTerm.getNext()==null)
            return currTerm;
        Term front = currTerm.getNext();
        int countSteps=0;
        do{
            front=front.getNext();
            if(front==null||countSteps>=10)
                return currTerm;
            countSteps++;
        } while(front.getText().charAt(front.getText().length()-1)!='"');
        if(countSteps<=3)
            return currTerm;
        Term result = mergeTerms(currTerm,countSteps+1);
        String resultText =result.getText().replace(" *","");
        resultText =resultText.replace('"'+"","");
        result.setText(resultText);
        result.setNumber(false);
        return result;
        //return mergeTerms(currTerm,countSteps+1);
    }
    /**
     * 2.3.3
     * merge terms in the terms chain that classify as Caps words
     * @param term
     * @return
     */
    public Term mergeCapsCase(Term term) {
        String text = term.getText();
        if(trimTerm(term)) {
            return term;
        }
        if(text.equals("I")||text.equals("I'm")) {
            return term;
        }
        int countSteps=0;
        Term front=null;
        front =term.getNext();
        //if the next term is in caps and it doesnt end with '.' or ','
        while(front!=null &&front.isCaps()) {
            if(trimTerm(front)) {
                countSteps++;
                break;
            }
            text = term.getText();
            if(text.length()>=2&&front.getText().length()>=2&&Character.isLowerCase(text.charAt(1))==Character.isLowerCase(front.getText().charAt(1)))
            { front = front.getNext();}
            else break;
            countSteps++;
        }
        return mergeTerms(term,countSteps);
    }
    /**
     * 2.3.4
     * merge terms in the terms chain that classify as Numbers terms
     * @param term
     * @return
     */
    private Term mergeNumberCase(Term term) {

        // % / $ percentage percent : . dollar
        if(term.getText().contains("%"))
            return term;
        if(term.getText().toLowerCase().contains("between"))
            if(term.getNext()!=null&&term.getNext().getNext()!=null&&term.getNext().getNext().getNext()!=null)
                if(term.getNext().isNumber()&&term.getNext().getNext().getText().toLowerCase().contains("and")&&term.getNext().getNext().getNext().isNumber()) { mergeTerms(term,3);
                    return term;
                }
        int countSteps=0;
        Term front = term.getNext();
        boolean stop=false;
        if(trimTerm(term))
            return term;
        while(front!=null&&(front.isNumber()||front.getText().contains("U.S"))&&!front.getText().contains("-"))
        {
            countSteps++;
            if(front!=null&&!front.getText().contains("U.S")&&trimTerm(front)) {
                stop=true;
                break;
            }
            front=front.getNext();
        }
        if(stop==false&&front!=null) {
            if ((front.getText().toLowerCase().contains("dollar")))
                countSteps++;
            if ((front.getText().toLowerCase().contains("percent")))
                countSteps++;
            if ((front.isDate()) && front.getPrev() != null && front.getPrev().isNumber()) {
                if(front.isDate())
                    term.setDate(true);
                countSteps++;
            }
        }
        return  mergeTerms(term,countSteps);
    }
    /**
     * 2.3.5
     * merge terms in the terms chain that classify as Dates
     * @param term
     * @return
     */
    public Term mergeDateCase(Term term) {
        if(term==null)
            return null;
        boolean doubleChain=false;
        if(term.getPrev()!=null&&term.getPrev().isNumber()&&trimTerm(term)) {
            mergeTerms(term.getPrev(), 1);
            term.getPrev().setDate(true);
            if (term.getNext() != null && term.getNext().isNumber()) {
                term.setText(term.getText() + " " + term.getNext().getText());
                doubleChain = true;
            }
        }
        if (term.getNext()!=null&&term.getNext().isNumber()&&!doubleChain) {
            mergeTerms(term,1);
            term.setDate(true);
        }
        term.setNumber(false);
        return term;
    }
    /**
     * 2.3. 2/3/4/5 .1
     * the method get terms and steps that indicate for how many next terms in the chain
     * that term need to be merge with
     * @param term
     * @param steps
     * @return
     */
    private Term mergeTerms(Term term,int steps) {

        if(steps==0)
            return term;
        Term first = term;
        ArrayList<Term> subTerms = new ArrayList<>();
        if(!term.isNumber()) {
            subTerms.add(new Term(term.getText()));
        }
        for (int i = 0; i <steps ; i++) {
            if(term.getNext()!=null && !term.getNext().isNumber())
                subTerms.add(new Term(term.getNext().getText()));
            if( term.getText().equals("$") ){
                term.setText(term.getText()+""+term.getNext().getText());
                term.setNext(term.getNext().getNext());
                if(term.getNext()!=null)
                    term.getNext().setPrev(term);
            }
            else{
                if(term.getNext().getText().equals("%")){
                    term.setText(term.getText()+""+term.getNext().getText());
                    term.setNext(term.getNext().getNext());
                    if(term.getNext()!=null)
                        term.getNext().setPrev(term);
                }
                else{
                    term.setText(term.getText()+" "+term.getNext().getText());
                    term.setNext(term.getNext().getNext());
                    if(term.getNext()!=null)
                        term.getNext().setPrev(term);
                }
            }
        }
        if(term.getPrev()!=null) {
            for (Term subTerm : subTerms) {
                chainTerms(term.getPrev(), subTerm);
            }
        }
        classifyTerm(term);
        return term;
        //        //<editor-fold desc="old merge terms">
//        //Term first = term;
//        //ArrayList<Term> subTerms = new ArrayList<>();
//
//        for (int i = 0; i <steps ; i++) {
//           // if(i!=0)
//            //    subTerms.add(new Term(term.getText()));
//            if( term.getText().equals("$") ){
//                term.setText(term.getText()+""+term.getNext().getText());
//                term.setNext(term.getNext().getNext());
//                if(term.getNext()!=null)
//                    term.getNext().setPrev(term);
//            }
//            else{
//                if(term.getNext().getText().equals("%")){
//                    term.setText(term.getText()+""+term.getNext().getText());
//                    term.setNext(term.getNext().getNext());
//                    if(term.getNext()!=null)
//                        term.getNext().setPrev(term);
//                }
//                else{
//                    term.setText(term.getText()+" "+term.getNext().getText());
//                    term.setNext(term.getNext().getNext());
//                    if(term.getNext()!=null)
//                        term.getNext().setPrev(term);
//                }
//
//            }
//        }
//       // for (Term subTerm:subTerms) {
//         //   chainTerms(first,subTerm);
//       // }
//        classifyTerm(term);
//        return term;
//        //</editor-fold>

    }
    /**
     * 2.3.6
     * the method clean the edges of the term from signs
     * @param term
     * @return
     */
    private boolean trimTerm(Term term) {
        boolean termed=false;
        if(term==null)
            return false;
        String termText = term.getText();
        if(termText.charAt(termText.length()-1)=='.'||
                termText.charAt(termText.length()-1)==','||
                termText.charAt(termText.length()-1)==':') {
            termText=termText.substring(0, termText.length() - 1);
            termed=true;
        }
        if(termText.length()>0) {
            if (termText.charAt(0) == '.' ||
                    termText.charAt(0) == ',' ||
                    termText.charAt(0) == ':') {
                termText = termText.substring(1);
                termed = true;
            }
        }
        return termed;
    }
    /**
     * 2.4
     * the method coordinates the entire parsing process.
     * call to the right method to parse by the right classification
     * @param firstTerm
     */
    public void ParseTermList(Term firstTerm){

        Term currentTerm = firstTerm;
        while(currentTerm!=null)
        {
            if(currentTerm.isDate()) {
                ParseDate(currentTerm);
            }
            if(currentTerm.isNumber()) {
                ParseNumber(currentTerm);
            }
            if(currentTerm.isCaps())
                ParseCaps(currentTerm);

            currentTerm=currentTerm.getNext();
        }

    }
    /**
     * 2.4.1
     * the method parse the term by the right rulse of date cases;
     * @param term
     */

    //<editor-fold desc="new ParseDate">
    private void ParseDate(Term term) {
        term.setNumber(false);
        String text = term.getText().toLowerCase();
        String[] subs = text.split(" ");
        if (subs.length != 2)
            return;
        String month = "";
        String day = "";
        if (Dates.containsKey(subs[0])) {
            month = subs[0];
            day = subs[1];
        } else if (Dates.containsKey(subs[1])) {
            month = subs[1];
            day = subs[0];
        } else
            return;
        //reduce month into a number
        while (Dates.containsKey(month))
            month = Dates.get(month);
        String[] days = day.split("-");
        if (days.length > 1) {
            //if day-day month
            for (int i = 0; i < days.length; i++) {
                days[i] = days[i] +"-"+ month;
            }
            term.setText(days[0]);//set with one of the dates
            Term nextDateTerm = new Term(days[1]);
            chainTerms(term,nextDateTerm);//add the second date
        } else if (day.length() > 2)//day is actually a year
            term.setText(month + "-" + day);
        else//day is actually a day
            term.setText(day + "-" + month);
    }
    //</editor-fold>

    //<editor-fold desc="old ParseDate">
    /*
    private void ParseDate(Term term) {
        term.setNumber(false);
        String text = term.getText();
        text = text.toUpperCase();

        String[] subs = text.split(" ");

        if( !(subs[0].contains("JAN") || subs[0].contains("DEC")|| subs[0].contains("FEB")|| subs[0].contains("MAR")||
                subs[0].contains("APR")|| subs[0].contains("MAY")|| subs[0].contains("JUN")|| subs[0].contains("AUG")||
                subs[0].contains("OCT")|| subs[0].contains("NOV")||subs[0].contains("SEP")||subs[0].contains("MAY") ) ){
            System.out.println();

            if(Integer.parseInt(subs[0])>31){
                //year-month  do nothing
                text = subs[0]+" "+subs[1];
            }
            else{
                if(subs[0].length()==1){
                    subs[0]="0"+subs[0];
                }
                //      month      day
                text = subs[1]+" "+subs[0];
            }
        }
        else{
            if(Integer.parseInt(subs[1])>31){
                text = subs[1]+" "+subs[0];
            }
            else{
                if(subs[1].length()==1){
                    subs[1]="0"+subs[1];
                }
                text = subs[0]+" "+subs[1];
            }
        }
        if(text.contains(" ")) {
            text=text.replace(' ', '-');
            if (text.contains("JANUARY"))
                text=text.replace("JANUARY", "01");
            else if (text.contains("FEBRUARY"))
                text=text.replace("FEBRUARY", "02");
            else if (text.contains("MARCH"))
                text=text.replace("MARCH", "03");
            else if (text.contains("APRIL"))
                text=text.replace("APRIL", "04");
            else if (text.contains("JUNE"))
                text=text.replace("JUNE", "06");
            else if (text.contains("JULY"))
                text=text.replace("JULY", "07");
            else if (text.contains("AUGUST"))
                text=text.replace("AUGUST", "08");
            else if (text.contains("SEPTEMBER"))
                text=text.replace("SEPTEMBER", "09");
            else if (text.contains("OCTOBER"))
                text=text.replace("OCTOBER", "10");
            else if (text.contains("DECEMBER"))
                text=text.replace("DECEMBER", "12");
            else if (text.contains("NOVEMBER"))
                text=text.replace("NOVEMBER", "11");
            else if (text.contains("DEC"))
                text=text.replace("DEC", "12");
            else if (text.contains("JAN"))
                text=text.replace("JAN", "01");
            else if (text.contains("FEB"))
                text=text.replace("FEB", "02");
            else if (text.contains("MAR"))
                text=text.replace("MAR", "03");
            else if (text.contains("APR"))
                text=text.replace("APR", "04");
            else if (text.contains("MAY"))
                text=text.replace("MAY", "05");
            else if (text.contains("JUN"))
                text=text.replace("JUN", "06");
            else if (text.contains("JUL"))
                text=text.replace("JUL", "07");
            else if (text.contains("AUG"))
                text=text.replace("AUG", "08");
            else if (text.contains("OCT"))
                text=text.replace("OCT", "10");
            else if (text.contains("NOV"))
                text=text.replace("NOV", "11");
            else if (text.contains("SEP"))
                text=text.replace("SEP", "09");

            term.setText(text);
        }
        else
            term.setDate(false);
    }
    */
    //</editor-fold>

    /**
     * 2.4.2
     * the method parse the term by the right rulse of number cases;
     * @param term
     */
    private void ParseNumber(Term term) {
        ///HAIM&LIOR'S FIVE STEP NUMBER PARSER///

        ///step 1: term classification
        ///price term -> suffix = dollars , save original number exp in numberholder in case the price is lower than M
        ///percent term -> suffix = %
        ///*the suffix will be used as an indicator in later steps

        ///step 2: literal form reduction
        ///save decimal '.' index in dotIndex
        /// ',' -> null
        ///'thousand' -> 'k' , dotIndex+=3
        ///'million' -> 'm' , dotIndex+=6
        ///'billion' -> 'bn' , dotIndex+=9

        ///step 3: expansion
        ///'k'->'000'
        ///'m'->'000000'
        ///'bn'->'000000000'

        ///step 4: decimal placing
        ///not price term & +9 figures -> dotIndex-=9 & inner_suffix = 'B'
        ///+6 figures -> dotIndex-=6 & inner_suffix = 'M'
        ///not price term & +3 figures -> dotIndex-=3 & inner_suffix = 'K'

        ///step 5: decimal correction
        ///while last char == '0', last char -> null (only if there is a decimal dot)
        ///add inner_suffix
        ///add suffix
        if(term.getText().toLowerCase().equals("million")|| term.getText().toLowerCase().equals("billion")||
                term.getText().toLowerCase().equals("thousand")){
            term.setText(term.getText().toLowerCase());
            return;
        }
        if(!term.getText().contains(" ") || term.isNumber()||term.isDate()|| term.isCaps()  ) {
            if (!(term.getText().contains("%") || term.getText().toLowerCase().contains("between"))) {
                String text = term.getText();
                String blank="";
                String acText = "";
                String acSuffix = "";
                String numberholder = "";
                String[] subs = text.split(" ");

                if (text.contains("$") || text.toLowerCase().contains("dollar") || text.toLowerCase().contains("u.s")) {
                    acSuffix = " Dollars";
                    blank= " ";
                    for (int i = 0; i < text.length(); i++) {
                        if ((text.charAt(i) >= '0' && text.charAt(i) <= '9') || text.charAt(i) == '.' || text.charAt(i) <= ',' || text.charAt(i) <= '/')
                            if (text.charAt(i) != '$')
                                numberholder += text.charAt(i);
                    }
                }
                for (int i = 0; i < subs.length; i++) {
                    String sub = subs[i];
                    if (sub.charAt(0) == '$') {
                        sub = sub.substring(1);
                        acSuffix = " Dollars";
                    }
                    if (!(sub.contains("U.S") || sub.toLowerCase().contains("dollar") || sub.toLowerCase().contains("percent") || sub.contains("%"))) {
                        if (i != subs.length - 1) {
                            if (subs[i + 1].toLowerCase().contains("thousand")) {
                                sub += "k";
                                i++;
                            } else if (subs[i + 1].toLowerCase().contains("billion")) {
                                sub += "bn";
                                i++;
                            } else if (subs[i + 1].toLowerCase().contains("million")) {
                                sub += "m";
                                i++;
                            }
                        }
                        int dotIndex = sub.length();
                        sub = sub.replaceAll(",", "");
                        if (sub.contains(".")) {
                            dotIndex = sub.indexOf('.');
                            if (sub.endsWith("bn")) {
                                sub = sub.replace("bn", "000000000");
                                dotIndex += 9;
                            } else if (sub.endsWith("m")) {
                                sub = sub.replace("m", "000000");
                                dotIndex += 6;
                            } else if (sub.endsWith("k")) {
                                sub = sub.replace("k", "000");
                                dotIndex += 3;
                            }
                            if( (i+1)<subs.length){
                                if(!(subs[i+1].contains("percent"))){
                                    sub = sub.replace(".", "");
                                }
                            }
                            sub = sub.substring(0, dotIndex) + "." + sub.substring(dotIndex);
                        } else {
                            if (sub.endsWith("k")) {
                                sub = sub.replace("k", "000");
                            } else if (sub.endsWith("m")) {
                                sub = sub.replace("m", "000000");
                            } else if (sub.endsWith("bn")) {
                                sub = sub.replace("bn", "000000000");
                            }
                        }
                        if (!sub.contains(".")) {
                            sub += ".";
                        }
                        /////////////////////////////////////////////////////////////////////////////////////
                        String suffix = "";
                        dotIndex = sub.indexOf(".");
                        if (acSuffix == "") {
                            if (dotIndex > 9) {
                                dotIndex -= 9;
                                suffix = blank+"B ";
                            } else if (dotIndex > 6) {
                                dotIndex -= 6;
                                suffix = blank+"M ";
                            } else if (dotIndex > 3) {
                                suffix = blank+"K ";
                                dotIndex -= 3;
                            }
                        } else {
                            if (dotIndex > 6) {
                                dotIndex -= 6;
                                suffix = blank+"M ";
                            }
                        }
                        ////////////////////////////////////////////////////////////////////////////////////
                        sub = sub.replace(".", "");
                        sub = sub.substring(0, dotIndex) + "." + sub.substring(dotIndex);
                        if (sub.endsWith("."))
                            sub = sub.substring(0, sub.length() - 1);
                        else {
                            if (sub.length() - sub.indexOf('.') > 3)
                                sub = sub.substring(0, sub.indexOf('.') + 4);
                            while (sub.endsWith("0"))
                                sub = sub.substring(0, sub.length() - 1);
                        }
                        if (sub.endsWith("."))
                            sub = sub.substring(0, sub.length() - 1);
                        sub += suffix;
                        acText += sub + " ";
                    } else {
                        if (sub.contains("$") || sub.toLowerCase().contains("dollar") || sub.contains("U.S"))
                            acSuffix = "Dollars";
                        else if (sub.toLowerCase().contains("percent") || sub.contains("%"))
                            acSuffix = "%";
                    }
                }//for
                if (acText.length() != 0)
                    acText = acText.substring(0, acText.length() - 1);
                acText += acSuffix;
                acText = acText.replaceAll("  ", " ");
                term.setText(acText);
                if (acSuffix != "" && acSuffix != "%")
                    if (acText.contains("M") || acText.contains("B")) {
                        term.setText(acText);
                    } else {
                        term.setText(numberholder + acSuffix);
                        acText = numberholder + acSuffix;
                    }
            }
            else if(term.getText().toLowerCase().contains("between")){
                if (term.getText().length() >= 7)
                    ParseBetween(term);
            }

        }else {
           /* if(term.getText().contains("/"))
            {
                sample.Term t = new sample.Term(term.getText().split(" ")[1]);
                t.setNext(term.getNext());
                term.setText(term.getText().split(" ")[0]);
                chainTerms(term,t);
            }*/
            term.setNumber(false);
        }
    }
    /**
     * 2.4.2.1
     * the method parse the term by the right rulse of terms tha contain the word between;
     * @param term
     */
    private void ParseBetween(Term term){
        String[] subs = term.getText().split(" ");
        if(subs.length!=4)
            return;
        String range="";
        if((subs[1].contains("0")||subs[1].contains("1")||subs[1].contains("2")
                ||subs[1].contains("3")||subs[1].contains("4")||subs[1].contains("5")
                ||subs[1].contains("6")||subs[1].contains("7")||subs[1].contains("8")
                ||subs[1].contains("9"))&&
                (subs[3].contains("0")||subs[3].contains("1")||subs[3].contains("2")
                        ||subs[3].contains("3")||subs[3].contains("4")||subs[3].contains("5")
                        ||subs[3].contains("6")||subs[3].contains("7")||subs[3].contains("8")
                        ||subs[3].contains("9")))
        {
            range=subs[1]+"-"+subs[3];
        }
        Term rangeTerm = new Term(range);
        Term prev = term.getPrev();
        chainTerms(term,rangeTerm);
    }
    /**
     * 2.4.3
     * the method parse the term by the right rulse of words in Caps state;
     * @param term
     */
    private void ParseCaps(Term term) {
        String text = term.getText();
        String[] parts = text.split(" ");
        if(stopWordsSet.contains(parts[0].toLowerCase()))
            parts[0]="";
        if(stopWordsSet.contains(parts[parts.length-1].toLowerCase()))
            parts[parts.length-1]="";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if(i!=parts.length-1)
                sb.append(parts[i]+' ');
            else
                sb.append(parts[i]);

        }
        term.setText(sb.toString());

    }
    /**
     * 2.5
     * the method traverse over the terms chain and check if it contain "stop words"
     * if contain the method remove the term from the terms chain
     * @param firstTerm
     */
    public Term removeStopWords(Term firstTerm) {
        Term currTerm = firstTerm;

        while(currTerm!=null)
        {
            if(stopWordsSet.contains(currTerm.getText())) {
                if (currTerm.getNext() != null)
                    currTerm.getNext().setPrev(currTerm.getPrev());
                if (currTerm.getPrev() != null)
                    currTerm.getPrev().setNext(currTerm.getNext());
                else{
                    firstTerm = firstTerm.getNext();
                    firstTerm.setPrev(null);
                }

            }
            else {
                //<editor-fold desc="replace colors">
                if(colorTable.containsKey(currTerm.getText())) {
                    //System.out.println(currTerm.getText() +"  "+colorTable.get(currTerm.getText()));
                    currTerm.setText(colorTable.get(currTerm.getText()));
                }
                ///</editor-fold>
                finalizeTerm(currTerm);
            }
            currTerm=currTerm.getNext();
        }
        return firstTerm;
    }
    /**
     * 2.5.1
     * the method get a term and "clean" its edeges from "junk" text
     * @param term
     */
    private void finalizeTerm(Term term) {
        String originalText = term.getText();
        String text = term.getText().toLowerCase();
        if(text.length()<2)
            return;
        int front=0;
        int back=text.length()-1;
        while(!((text.charAt(front) >= 'a' && text.charAt(front) <= 'z')||(text.charAt(front) >= '0' && text.charAt(front) <= '9'))) {
            front++;
            if (front == back) {
                front = 0;
                break;
            }
        }
        if(!text.endsWith("%")){
            while(! ((text.charAt(back) >= 'a' && text.charAt(back) <= 'z')||(text.charAt(back) >= '0' && text.charAt(back) <= '9')) ) {
                back--;
                if (front == back) {
                    back = text.length()-1;
                    break;
                }
            }
        }
        text = originalText.substring(front,back+1);
        if(CapitalCharacters.contains(text.charAt(0)))
            text = text.toUpperCase();
        else
            text.toLowerCase();
        // System.out.println(text + " "+term.isNumber()+ " "+term.isDate());
        term.setText(text);
    }
    /**
     * 1.1
     * the method Initilize all the dictionaries that the class use in the parsing process
     */
    private void InitilizeDictionaries() {
        //<editor-fold desc="junkChars">
        junkChars = new ArrayList<String>(
                Arrays.asList("..","#","!","@","=","+","*","^",";","'","--","$$","%%"));
        //</editor-fold>
        //<editor-fold desc="delimiters">
        Delimiters = new ArrayList<Character>(
                Arrays.asList('_','�',"'".charAt(0),' ',';',
                        '(',')','[',']','{','}'
                        ,'`','~','<','>',
                        '!','?','^','*','|'
                        ,'\t','\n'));
        //</editor-fold>
        //<editor-fold desc="seperators">
        seperators = new ArrayList<Character>(
                Arrays.asList('.',':',
                        '-','|','`','+',
                        '@','#','$','\\','/','&'));
        //</editor-fold>
        //<editor-fold desc="Dates">
        Dates = new Hashtable<>();
        //full month name
        Dates.put("january","jan");
        Dates.put("february","feb");
        Dates.put("march","mar");
        Dates.put("april","apr");
        Dates.put("june","jun");
        Dates.put("july","jul");
        Dates.put("august","aug");
        Dates.put("september","sep");
        Dates.put("october","oct");
        Dates.put("november","nov");
        Dates.put("december","dec");
        //short month name
        Dates.put("jan","01");
        Dates.put("feb","02");
        Dates.put("mar","03");
        Dates.put("apr","04");
        Dates.put("may","05");
        Dates.put("jun","06");
        Dates.put("jul","07");
        Dates.put("aug","08");
        Dates.put("sep","09");
        Dates.put("oct","10");
        Dates.put("nov","11");
        Dates.put("dec","12");
//        Dates = new ArrayList<String>(
//                Arrays.asList(
//                        "JAN","JANUARY",
//                        "FEB","FEBRUARY",
//                        "MAR","MARCH",
//                        "APR","APRIL",
//                        "MAY",
//                        "JUN","JUNE",
//                        "JUL","JULY",
//                        "AUG","AUGUST",
//                        "SEP","SEPTEMBER",
//                        "OCT","OCTOBER",
//                        "NOV","NOVEMBER",
//                        "DEC","DECEMBER"));
        //</editor-fold>
        //<editor-fold desc="numbers">
        Numbers = new ArrayList<String>(
                Arrays.asList(
                        "between",
                        "0","1","2","3","4","5","6","7","8","9",
                        "thousand","million","billion"));
        //</editor-fold>
        //<editor-fold desc="CapitalCharacters">
        CapitalCharacters = new HashSet<>();
        for (int i = 'A'; i <= 'Z'; i++) {
            CapitalCharacters.add((char)i);
        }
        //</editor-fold>
        //<editor-fold desc="colors">
        colorTable = new Hashtable<>();
        //<editor-fold desc="black">
        colorTable.put("brunet","black");
        colorTable.put("charcoal","black");
        colorTable.put("clouded","black");
        colorTable.put("coal","black");
        colorTable.put("ebony","black");
        colorTable.put("jet","black");
        colorTable.put("obsidian","black");
        colorTable.put("onyx","black");
        colorTable.put("pitch","black");
        colorTable.put("raven","black");
        colorTable.put("sable","black");
        colorTable.put("sloe","black");
        colorTable.put("dusky","black");
        colorTable.put("ebon","black");
        colorTable.put("inklike","black");
        colorTable.put("livid","black");
        colorTable.put("melanoid","black");
        colorTable.put("murky","black");
        colorTable.put("pitch-dark","black");
        colorTable.put("shadowy","black");
        colorTable.put("sombre","black");
        colorTable.put("sooty","black");
        colorTable.put("starless","black");
        colorTable.put("stygian","black");
        colorTable.put("swart","black");
        colorTable.put("swarthy","black");
        //</editor-fold>
        //<editor-fold desc="grey">
        colorTable.put("drab","grey");
        colorTable.put("dusty","grey");
        colorTable.put("silvery","grey");
        colorTable.put("Dove","grey");
        colorTable.put("ash","grey");
        colorTable.put("clouded","grey");
        colorTable.put("dappled","grey");
        colorTable.put("heather","grey");
        colorTable.put("iron","grey");
        colorTable.put("lead","grey");
        colorTable.put("oyster","grey");
        colorTable.put("pearly","grey");
        colorTable.put("powder","grey");
        colorTable.put("shaded","grey");
        colorTable.put("silvered","grey");
        colorTable.put("slate","grey");
        colorTable.put("stone","grey");
        colorTable.put("ashen","grey");
        colorTable.put("dingy","grey");
        colorTable.put("dusky","grey");
        colorTable.put("leaden","grey");
        colorTable.put("livid","grey");
        colorTable.put("mousy","grey");
        colorTable.put("peppery","grey");
        colorTable.put("sere","grey");
        colorTable.put("smoky","grey");
        colorTable.put("somber","grey");
        //</editor-fold>
        //<editor-fold desc="white">
        colorTable.put("silver","white");
        colorTable.put("silvery","white");
        colorTable.put("alabaster","white");
        colorTable.put("blanched","white");
        colorTable.put("bleached","white");
        colorTable.put("frosted","white");
        colorTable.put("ivory","white");
        colorTable.put("light","white");
        colorTable.put("pasty","white");
        colorTable.put("pearly","white");
        colorTable.put("wan","white");
        colorTable.put("achromatic","white");
        colorTable.put("achromic","white");
        colorTable.put("ashen","white");
        colorTable.put("bloodless","white");
        colorTable.put("chalky","white");
        colorTable.put("ghastly","white");
        colorTable.put("hoary","white");
        colorTable.put("milky","white");
        colorTable.put("pallid","white");
        colorTable.put("snowy","white");
        colorTable.put("waxen","white");
        //</editor-fold>
        //<editor-fold desc="red">
        colorTable.put("cardinal","red");
        colorTable.put("coral","red");
        colorTable.put("crimson","red");
        colorTable.put("flaming","red");
        colorTable.put("glowing","red");
        colorTable.put("maroon","red");
        colorTable.put("rose","red");
        colorTable.put("wine","red");
        colorTable.put("bittersweet","red");
        colorTable.put("blooming","red");
        colorTable.put("blush","red");
        colorTable.put("brick","red");
        colorTable.put("burgundy","red");
        colorTable.put("carmine","red");
        colorTable.put("cerise","red");
        colorTable.put("cherry","red");
        colorTable.put("chestnut","red");
        colorTable.put("claret","red");
        colorTable.put("copper","red");
        colorTable.put("dahlia","red");
        colorTable.put("fuchsia","red");
        colorTable.put("garnet","red");
        colorTable.put("geranium","red");
        colorTable.put("infrared","red");
        colorTable.put("magenta","red");
        colorTable.put("pink","red");
        colorTable.put("puce","red");
        colorTable.put("ruby","red");
        colorTable.put("russet","red");
        colorTable.put("rust","red");
        colorTable.put("salmon","red");
        colorTable.put("sanguine","red");
        colorTable.put("scarlet","red");
        colorTable.put("titian","red");
        colorTable.put("vermilion","red");
        colorTable.put("bloodshot","red");
        colorTable.put("florid","red");
        colorTable.put("flushed","red");
        colorTable.put("healthy","red");
        colorTable.put("inflamed","red");
        colorTable.put("roseate","red");
        colorTable.put("rosy","red");
        colorTable.put("rubicund","red");
        colorTable.put("ruddy","red");
        colorTable.put("rufescent","red");
        //</editor-fold>
        // <editor-fold desc="orange">
        colorTable.put("apricot","orange");
        colorTable.put("bittersweet","orange");
        colorTable.put("cantaloupe","orange");
        colorTable.put("carrot","orange");
        colorTable.put("coral","orange");
        colorTable.put("peach","orange");
        colorTable.put("salmon","orange");
        colorTable.put("tangerine","orange");
        colorTable.put("titian","orange");
        colorTable.put("red-yellow","orange");
        //</editor-fold>
        //<editor-fold desc="yellow">
        colorTable.put("amber","yellow");
        colorTable.put("bisque","yellow");
        colorTable.put("blond","yellow");
        colorTable.put("buff","yellow");
        colorTable.put("chrome","yellow");
        colorTable.put("cream","yellow");
        colorTable.put("gold","yellow");
        colorTable.put("ivory","yellow");
        colorTable.put("lemon","yellow");
        colorTable.put("saffron","yellow");
        colorTable.put("sand","yellow");
        colorTable.put("tawny","yellow");
        //</editor-fold>
        //<editor-fold desc="green">
        colorTable.put("field","green");
        colorTable.put("grass","green");
        colorTable.put("lawn","green");
        colorTable.put("common","green");
        colorTable.put("plaza","green");
        colorTable.put("sward","green");
        colorTable.put("terrace","green");
        colorTable.put("turf","green");
        colorTable.put("grassplot","green");
        //</editor-fold>
        //<editor-fold desc="blue">
        colorTable.put("azure","blue");
        colorTable.put("beryl","blue");
        colorTable.put("cerulean","blue");
        colorTable.put("cobalt","blue");
        colorTable.put("indigo","blue");
        colorTable.put("navy","blue");
        colorTable.put("royal","blue");
        colorTable.put("sapphire","blue");
        colorTable.put("teal","blue");
        colorTable.put("turquoise","blue");
        colorTable.put("ultramarine","blue");
        //</editor-fold>
        //<editor-fold desc="purple">
        colorTable.put("lavender","purple");
        colorTable.put("lilac","purple");
        colorTable.put("mauve","purple");
        colorTable.put("periwinkle","purple");
        colorTable.put("plum","purple");
        colorTable.put("violet","purple");
        colorTable.put("amethyst","purple");
        colorTable.put("heliotrope","purple");
        colorTable.put("magenta","purple");
        colorTable.put("mulberry","purple");
        colorTable.put("orchid","purple");
        colorTable.put("pomegranate","purple");
        colorTable.put("wine","purple");
        colorTable.put("amaranthine","purple");
        colorTable.put("bluish red","purple");
        colorTable.put("perse","purple");
        colorTable.put("reddish blue","purple");
        colorTable.put("violaceous","purple");
        //</editor-fold>
        //<editor-fold desc="brown">
        colorTable.put("amber","brown");
        colorTable.put("bay","brown");
        colorTable.put("beige","brown");
        colorTable.put("bister","brown");
        colorTable.put("brick","brown");
        colorTable.put("bronze","brown");
        colorTable.put("buff","brown");
        colorTable.put("chestnut","brown");
        colorTable.put("chocolate","brown");
        colorTable.put("cinnamon","brown");
        colorTable.put("cocoa","brown");
        colorTable.put("coffee","brown");
        colorTable.put("copper","brown");
        colorTable.put("drab","brown");
        colorTable.put("dust","brown");
        colorTable.put("ecru","brown");
        colorTable.put("fawn","brown");
        colorTable.put("ginger","brown");
        colorTable.put("hazel","brown");
        colorTable.put("henna","brown");
        colorTable.put("khaki","brown");
        colorTable.put("mahogany","brown");
        colorTable.put("nut","brown");
        colorTable.put("ochre","brown");
        colorTable.put("puce","brown");
        colorTable.put("russet","brown");
        colorTable.put("rust","brown");
        colorTable.put("sepia","brown");
        colorTable.put("sorrel","brown");
        colorTable.put("tan","brown");
        colorTable.put("toast","brown");
        colorTable.put("umber","brown");
        colorTable.put("auburn","brown");
        colorTable.put("burnt sienna","brown");
        colorTable.put("snuff-colored","brown");
        colorTable.put("tawny","brown");
        colorTable.put("terra-cotta","brown");
        //</editor-fold>
        //</editor-fold>
    }
    /**
     * 2.4.2.1.1
     * the method get two terms the first int he terms chain and the other is not,
     * it fush the second infront of the first into the terms chain
     * @param term1
     * @param term2
     */
    private void chainTerms(Term term1,Term term2) {
        if(term1.getNext()==null) {
            term1.setNext(term2);
            term2.setPrev(term1);
        }else {
            // term1->next
            term2.setNext(term1.getNext());//term2->next
            term1.getNext().setPrev(term2);//term2<-next
            term2.setPrev(term1);//term1->term2
            term1.setNext(term2);//term1<-term2
        }
    }
    //</editor-fold>

    //<editor-fold desc="part B">
    public ArrayList<Character> getParseDelimiters(){
        return Delimiters;
    }
    public HashSet<String> getParserStopWordsSet(){
        return stopWordsSet;
    }
    //</editor-fold>
}

