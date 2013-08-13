package gov.nysenate.openleg.model;


import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnore;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;


@XStreamAlias("calendarEntry")
public class CalendarEntry
{
    /**
     * The entry's unique id
     */
    @XStreamAsAttribute
    private String oid;

    /**
     * The unique calendar number for this entry. This is the same for this entry on
     * all calendars during a calendar year.
     */
    @XStreamAsAttribute
    private String no = "";

    /**
     * The original bill for this calendar entry.
     */
    private Bill bill = null;

    /**
     * The substituted bill for this calendar entity. null if not substituted.
     */
    private Bill subBill = null;

    /**
     * "HIGH" if bill has not yet properly aged.
     */
    private String billHigh = "";

    /**
     *
     */
    private Date motionDate = null;

    /**
     *
     */
    private Section section = null;

    /**
     *
     */
    private Sequence sequence = null;

    /**
     * JavaBean Constructor
     */
    public CalendarEntry()
    {

    }

    /**
     * Fully constructs a calendar entry.
     *
     * @param no
     * @param high
     * @param motionDate
     * @param bill
     * @param subBill
     */
    public CalendarEntry(String no, String high, Date motionDate, Bill bill, Bill subBill)
    {
        this.setNo(no);
        this.setBillHigh(high);
        this.setMotionDate(motionDate);
        this.setBill(bill);
        this.setSubBill(subBill);
    }

    /**
     *
     * @return
     */
    public String getOid()
    {
        return this.oid;
    }

    /**
     *
     * @param oid
     */
    public void setOId(String oid)
    {
        this.oid = oid;
    }

    /**
     *
     * @return
     */
    public String getBillHigh()
    {
        return billHigh;
    }

    /**
     *
     * @param billHigh
     */
    public void setBillHigh(String billHigh)
    {
        this.billHigh = billHigh;
    }

    /**
     *
     * @return
     */
    @JsonIgnore
    public Sequence getSequence()
    {
        return sequence;
    }

    /**
     *
     * @param sequence
     */
    public void setSequence(Sequence sequence)
    {
        this.sequence = sequence;
    }

    /**
     *
     * @return
     */
    @JsonIgnore
    public Section getSection()
    {
        return section;
    }

    /**
     *
     * @param section
     */
    public void setSection(Section section)
    {
        this.section = section;
    }

    /**
     *
     * @return
     */
    public Date getMotionDate()
    {
        return motionDate;
    }

    /**
     *
     * @param motionDate
     */
    public void setMotionDate(Date motionDate)
    {
        this.motionDate = motionDate;
    }

    /**
     *
     * @return
     */
    public Bill getBill()
    {
        return bill;
    }

    /**
     *
     * @param bill
     */
    public void setBill(Bill bill)
    {
        this.bill = bill;
    }

    /**
     *
     * @return
     */
    public Bill getSubBill()
    {
        return subBill;
    }

    /**
     *
     * @param subBill
     */
    public void setSubBill(Bill subBill)
    {
        this.subBill = subBill;
    }

    /**
     *
     * @return
     */
    public String getNo()
    {
        return no;
    }

    /**
     *
     * @param no
     */
    public void setNo(String no)
    {
        this.no = no;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj != null && obj instanceof CalendarEntry) {
            CalendarEntry other = (CalendarEntry) obj;
            return other.getOid().equals(this.getOid());
        }
        else {
            return false;
        }
    }

    /**
     * @deprecated - Only kept around for old json serializers
     */
    @Deprecated
    public String getId()
   {
        return oid;
    }

    /**
     * @deprecated - Only kept around for old json serializers
     */
    @Deprecated
    public void setId(String id)
    {
        this.oid = id;
    }
}
