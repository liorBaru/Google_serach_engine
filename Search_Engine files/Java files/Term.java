package sample;

public class Term {

    private String Text;
    //<editor-fold desc="canceld position">
    //private int Position;
    //</editor-fold>
    private boolean isNumber;// e.g one million , 2 3/4 , 40 thousand
    private boolean isCaps; //e.g Daniel or THE DOLLAR
    private boolean isSeparated;//e.g full-moon or U.S.A or big/large or 12.3.59 or arms,legs,eyes,...
    private boolean isDate;
    private Term next;
    private Term prev;

    public Term(String text){
        //<editor-fold desc="canceld position">
        //Position = -1;
        //</editor-fold>
        Text = text;
        isNumber=false;
        isCaps=false;
        isSeparated=false;
    }
    /**
     * getter for previous sample.Term
     * @return
     */
    public Term getPrev() {
        return prev;
    }
    /**
     * setter for previous sample.Term
     * @param prev
     */
    public void setPrev(Term prev) {
        this.prev = prev;
    }
    /**
     * getter for next sample.Term
     * @return
     */
    public Term getNext() { return next; }
    /**
     * setter for next sample.Term
     * @param next
     */
    public void setNext(Term next) {
        this.next = next;
    }
    /**
     * getter for isDate
     * @return
     */
    public boolean isDate() { return isDate;}
    /**
     * setter for isDate
     * @param date
     */
    public void setDate(boolean date) {
        isDate = date;
    }

    /**
     *
     * @return string of readable information about the term
     */
    public String getClassification() {
        String s="";
        if(isNumber) s+=" Number";
        if(isDate) s+=" Date";
        if(isCaps) s+=" Caps";
        if(isSeparated) s+=" Seperated";
        return s;
    }

    /**
     * getter for text
     * @return
     */
    public String getText() {
        return Text;
    }
    /**
     * setter for text
     * @param text
     */
    public void setText(String text) {
        Text = text;
    }

    //<editor-fold desc="canceld position">
    /* public int getPosition() {
        return Position;
    }
    public void setPosition(int position) {
        Position = position;
    }*/
    //</editor-fold>

    /**
     *
     * @return returns if term contains number
     */
    public boolean isNumber() {
        return isNumber;
    }
    /**
     * setter for isNumber
     * @param number
     */
    public void setNumber(boolean number) {
        isNumber = number;
    }
    /**
     * @return returns if term contains capital letters
     *
     */
    public boolean isCaps() { return isCaps; }
    /**
     * setter for isCaps
     * @param caps
     */
    public void setCaps(boolean caps) {
        isCaps = caps;
    }
    /**
     * @return returns if term contains separators
     *
     */
    public boolean isSeparated() {
        return isSeparated;
    }
    /**
     * setter for isSeparated
     * @param separated
     */
    public void setSeparated(boolean separated) {
        isSeparated = separated;
    }

}
