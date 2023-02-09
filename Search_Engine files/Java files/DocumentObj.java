package sample;

import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;

public class DocumentObj{

    private String Title;
    private String Date;
    private String ID;
    private String Text;
    private Term term;

    /**
     * default constructor
     */
    public DocumentObj(){
        this.Title = "Title";
        this.Date = "Date";
        this.ID = "ID";
        this.Text = "Text";
    }
    /**
     *
     * @param Title title of th document
     * @param ID id of the document
     * @param Date publish date of the documents
     * @param Text the content of the document
     */
    public DocumentObj(String Title,String ID,String Date,String Text){
        this.Title = Title;
        this.Date = Date;
        this.ID = ID;
        this.Text = Text;
    }
    /**
     * constructor which creates a Document from a String
     * @param Doc
     */
    public DocumentObj(String Doc) {
        if(Doc!=null&&!Doc.equals("")) {
            Document document = Jsoup.parse(Doc);
            ID = document.select("DOCNO").first().text();
            if (document.select("TI") != null && document.select("TI").first() != null)
                Title = document.select("TI").first().text();
            else
                Title = "untiteld document___________________________________________________________________________________";
            if (document.select("DATE1") != null && document.select("DATE1").first() != null)
                Date = document.select("DATE1").first().text();
            else if (document.select("DATE") != null && document.select("DATE").first() != null)
                Date = document.select("DATE").first().text();
            if (document.select("TEXT") != null && document.select("TEXT").first() != null)
                Text = document.select("TEXT").first().text();
            else
                Text = null;
        }
    }
    /**
     * static function for creating new sample.DocumentObj objects
     * @param Doc
     * @return
     */
    public static DocumentObj createDocument(String Doc){
        return new DocumentObj(Doc);
    }
    /**
     * getter of text of the document
     * @return
     */
    public String getText() {
        return Text;
    }
    /**
     *  setter for document text
      * @param text
     * @return
     */
    public boolean setText(String text) {
        Text = text;
        return true;
    }
    /**
     * getter of the first sample.Term in the document
     * @return
     */
    public Term getTerms() {
        return term;
    }
    /**
     * setter for the first sample.Term
     * @return
     */
    public boolean setTerms(Term firstTerm) {
        term = firstTerm;
        return true;
    }
    /**
     * getter of title
     * @return
     */
    public String getTitle() {
        return Title;
    }
    /**
     * getter of date
     * @return
     */
    public String getDate() {
        return Date;
    }
    /**
     * getter of ID
     * @return
     */
    public String getID() {
        return ID;
    }
}
