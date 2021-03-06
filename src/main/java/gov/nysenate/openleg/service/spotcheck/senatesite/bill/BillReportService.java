package gov.nysenate.openleg.service.spotcheck.senatesite.bill;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import gov.nysenate.openleg.dao.base.LimitOffset;
import gov.nysenate.openleg.dao.bill.data.BillUpdatesDao;
import gov.nysenate.openleg.dao.bill.reference.senatesite.SenateSiteDao;
import gov.nysenate.openleg.dao.spotcheck.BillIdSpotCheckReportDao;
import gov.nysenate.openleg.dao.spotcheck.SpotCheckReportDao;
import gov.nysenate.openleg.model.base.PublishStatus;
import gov.nysenate.openleg.model.bill.BaseBillId;
import gov.nysenate.openleg.model.bill.Bill;
import gov.nysenate.openleg.model.bill.BillId;
import gov.nysenate.openleg.model.spotcheck.*;
import gov.nysenate.openleg.model.spotcheck.senatesite.SenateSiteDump;
import gov.nysenate.openleg.model.spotcheck.senatesite.SenateSiteDumpId;
import gov.nysenate.openleg.model.spotcheck.senatesite.bill.SenateSiteBill;
import gov.nysenate.openleg.service.bill.data.BillDataService;
import gov.nysenate.openleg.service.bill.data.BillNotFoundEx;
import gov.nysenate.openleg.service.spotcheck.base.BaseSpotCheckReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BillReportService extends BaseSpotCheckReportService<BillId> {

    private static final Logger logger = LoggerFactory.getLogger(BillReportService.class);

    @Autowired private BillIdSpotCheckReportDao billReportDao;
    @Autowired private SenateSiteDao senateSiteDao;
    @Autowired private BillJsonParser billJsonParser;

    @Autowired private BillDataService billDataService;
    @Autowired private BillUpdatesDao billUpdatesDao;

    @Autowired private BillCheckService billCheckService;

    @Override
    protected SpotCheckReportDao<BillId> getReportDao() {
        return billReportDao;
    }

    @Override
    public SpotCheckRefType getSpotcheckRefType() {
        return SpotCheckRefType.SENATE_SITE_BILLS;
    }

    @Override
    public synchronized SpotCheckReport<BillId> generateReport(LocalDateTime start, LocalDateTime end) throws Exception {
        SenateSiteDump billDump = getMostRecentDump();
        SenateSiteDumpId dumpId = billDump.getDumpId();
        SpotCheckReportId reportId = new SpotCheckReportId(SpotCheckRefType.SENATE_SITE_BILLS,
                dumpId.getDumpTime(), LocalDateTime.now());
        SpotCheckReport<BillId> report = new SpotCheckReport<>(reportId);
        report.setNotes(billDump.getDumpId().getNotes());
        try {

            logger.info("getting bill updates");

            // Get reference bills using the bill dump update interval
            Set<BaseBillId> updatedBillIds = getBillUpdatesDuring(billDump);
            logger.info("got {} updated bill ids", updatedBillIds.size());
            Map<BaseBillId, Bill> updatedBills = new LinkedHashMap<>();
            logger.info("retrieving bills");
            for (BaseBillId billId : updatedBillIds) {
                try {
                    updatedBills.put(billId, billDataService.getBill(billId));
                } catch (BillNotFoundEx ex) {
                    SpotCheckObservation<BillId> observation = new SpotCheckObservation<>(reportId.getReferenceId(), billId);
                    observation.addMismatch(new SpotCheckMismatch(SpotCheckMismatchType.OBSERVE_DATA_MISSING, "", billId));
                    report.addObservation(observation);
                }
            }
            logger.info("got {} bills", updatedBills.size());
            logger.info("retrieving bill dump");
            // Extract senate site bills from the dump
            Multimap<BaseBillId, SenateSiteBill> dumpedBills = ArrayListMultimap.create();
            billJsonParser.parseBills(billDump).forEach(b -> dumpedBills.put(b.getBaseBillId(), b));
            logger.info("parsed {} dumped bills", dumpedBills.size());

            logger.info("comparing bills present");
            // Add observations for any missing bills that should have been in the dump
            report.addObservations(getRefDataMissingObs(dumpedBills.values(), updatedBills.values(),
                    reportId.getReferenceId()));

            logger.info("checking bills");
            // Check each dumped senate site bill
            dumpedBills.values().stream()
                    .map(senSiteBill -> billCheckService.check(updatedBills.get(senSiteBill.getBaseBillId()), senSiteBill))
                    .forEach(report::addObservation);

            logger.info("done: {} mismatches", report.getOpenMismatchCount(false));
        } finally {
            logger.info("archiving bill dump...");
            senateSiteDao.setProcessed(billDump);
        }
        return report;
    }

    /** --- Internal Methods --- */

    private SenateSiteDump getMostRecentDump() throws IOException, ReferenceDataNotFoundEx {
        return senateSiteDao.getPendingDumps(SpotCheckRefType.SENATE_SITE_BILLS).stream()
                .filter(SenateSiteDump::isComplete)
                .max(SenateSiteDump::compareTo)
                .orElseThrow(() -> new ReferenceDataNotFoundEx("Found no full senate site bill dumps"));
    }

    /**
     * Gets a set of bill ids that were updated during the update interval specified by the bill dump
     *
     * @param billDump SenateSiteBillDump
     * @return Set<Bill>
     */
    private Set<BaseBillId> getBillUpdatesDuring(SenateSiteDump billDump) {
        SenateSiteDumpId dumpId = billDump.getDumpId();
        logger.info("Getting Openleg Bills for session: {}", dumpId.getSession());
        return new TreeSet<>(
                billDataService.getBillIds(dumpId.getSession(), LimitOffset.ALL)
        );
    }

    /**
     * Generate data missing observations for all bills that were updated in the bill dump update interval,
     *  but not included in the bill dump
     * @param senSiteBills Collection<SenateSiteBill> - Bills extracted from the dump
     * @param openlegBills Collection<Bill> - Bills updated during the dump interval
     * @param refId SpotCheckReferenceId - reference Id used to create the observations
     * @return List<SpotCheckObservation<BillId>>
     */
    private List<SpotCheckObservation<BillId>> getRefDataMissingObs(Collection<SenateSiteBill> senSiteBills,
                                                                    Collection<Bill> openlegBills,
                                                                    SpotCheckReferenceId refId) {
        Set<BillId> senSiteBillIds = senSiteBills.stream()
                .map(SenateSiteBill::getBillId)
                .collect(Collectors.toSet());
        Set<BillId> openlegBillIds = openlegBills.stream()
                .flatMap(bill -> bill.getAmendmentIds().stream()
                        .filter(billId -> bill.getPublishStatus(billId.getVersion())
                                .map(PublishStatus::isPublished)
                                .orElse(false)))
                .collect(Collectors.toSet());
        return Sets.difference(openlegBillIds, senSiteBillIds).stream()
                .map(billId -> {
                    SpotCheckObservation<BillId> observation = new SpotCheckObservation<>(refId, billId);
                    observation.addMismatch(new SpotCheckMismatch(SpotCheckMismatchType.REFERENCE_DATA_MISSING, "", ""));
                    return observation;
                })
                .collect(Collectors.toList());
    }
}
