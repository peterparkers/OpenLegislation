package gov.nysenate.openleg.client.view.bill;

import gov.nysenate.openleg.client.view.base.ViewObject;
import gov.nysenate.openleg.model.bill.Bill;

/**
 * A complete representation of a bill including it's amendments.
 */
public class FullBillView extends SimpleBillView implements ViewObject
{
    protected String lawSection;
    protected String law;


    protected String programInfo;

    public FullBillView(Bill bill) {
        super(bill);
    }
}
