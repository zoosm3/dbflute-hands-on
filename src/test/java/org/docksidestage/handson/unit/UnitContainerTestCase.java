package org.docksidestage.handson.unit;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

import javax.sql.DataSource;

import org.dbflute.cbean.result.ListResultBean;
import org.dbflute.helper.HandyDate;
import org.dbflute.utflute.lastadi.ContainerTestCase;
import org.docksidestage.handson.dbflute.exbhv.MemberBhv;
import org.docksidestage.handson.dbflute.exbhv.PurchaseBhv;
import org.docksidestage.handson.dbflute.exentity.Member;
import org.docksidestage.handson.dbflute.exentity.Purchase;

/**
 * The base class of unit test cases with DI container. <br />
 * Use like this:
 * <pre>
 * ... YourTest <span style="color: #FD4747">extends</span> {@link UnitContainerTestCase} {
 * 
 *     public void test_yourMethod() {
 *         <span style="color: #3F7E5E">// ## Arrange ##</span>
 *         YourAction action = new YourAction();
 *         <span style="color: #FD4747">inject</span>(action);
 * 
 *         <span style="color: #3F7E5E">// ## Act ##</span>
 *         action.submit();
 * 
 *         <span style="color: #3F7E5E">// ## Assert ##</span>
 *         assertTrue(action...);
 *     }
 * }
 * </pre>
 * @author jflute
 */
public abstract class UnitContainerTestCase extends ContainerTestCase {

    private MemberBhv memberBhv;
    private PurchaseBhv purchaseBhv;

    /**
     * Adjust member's formalized date-time by keyword of member name. <br />
     * This method updates the first member that has the keyword in ordered id.
     * <pre>
     * e.g. update member that contains 'vi' in name
     *  adjustMember_FormalizedDatetime_FirstOnly(toDate("2005/10/05"), "vi")
     * </pre>
     * @param formalizedDatetime The date-time when the member was formalized. (NullAllowed: if null, update as null)
     * @param nameKeyword The keyword as contain-search to search members updated. (NotNull)
     * @param limit The limit count of updated member. (NotMinus & NotZero)
     */
    protected void adjustMember_FormalizedDatetime_FirstOnly(LocalDateTime formalizedDatetime, String nameKeyword) {
        assertNotNull(nameKeyword);
        Member member = memberBhv.selectEntityWithDeletedCheck(cb -> {
            cb.query().setMemberName_LikeSearch(nameKeyword, op -> op.likeContain());
            cb.query().addOrderBy_MemberId_Asc();
            cb.fetchFirst(1);
        });
        member.setFormalizedDatetime(formalizedDatetime);
        memberBhv.updateNonstrict(member);
    }

    /**
     * Adjust the purchase date-time of the fixed member. <br />
     * Update the date-time as the member's formalized date-time in a week. <br />
     * This is for the seventh exercise of section 3. <br />
     * You can get the target data that has border line.
     */
    protected void adjustPurchase_PurchaseDatetime_fromFormalizedDatetimeInWeek() {
        // not to select existing in-week data
        LocalDateTime fromDate = toLocalDateTime("2005-01-01");
        LocalDateTime toDate = toLocalDateTime("2007-01-01");
        Member adjustedMember = memberBhv.selectEntityWithDeletedCheck(cb -> {
            cb.query().existsPurchase(purchaseCB -> {});
            cb.query().setFormalizedDatetime_FromTo(fromDate, toDate, op -> op.compareAsDate());
            cb.fetchFirst(1);
        });

        ListResultBean<Purchase> updatedPurchaseList = purchaseBhv.selectList(cb -> {
            cb.query().setMemberId_Equal(adjustedMember.getMemberId());
            cb.query().scalar_Equal().max(purCB -> {
                purCB.specify().columnPurchaseDatetime();
            }).partitionBy(purCB -> {
                purCB.specify().columnMemberId();
            });
        });
        HandyDate handyDate = new HandyDate(adjustedMember.getFormalizedDatetime(), getUnitTimeZone());
        LocalDateTime movedDatetime = handyDate.addDay(7).moveToDayTerminal().moveToSecondJust().getLocalDateTime();
        updatedPurchaseList.forEach(purchase -> {
            purchase.setPurchaseDatetime(movedDatetime);
        });
        purchaseBhv.batchUpdate(updatedPurchaseList);
    }

    /**
     * Adjust transaction isolation level to READ COMMITTED on this session. <br />
     * This method depends on the MySQL. (you cannot use for other DBMSs)
     * @throws SQLException
     */
    protected void adjustTransactionIsolationLevel_ReadCommitted() throws SQLException {
        DataSource dataSource = getDataSource();
        Connection conn = null;
        Statement st = null;
        try {
            conn = dataSource.getConnection();
            st = conn.createStatement();
            st.execute("set SESSION transaction isolation level READ COMMITTED");
        } finally {
            if (st != null) {
                st.close();
            }
            if (conn != null) {
                conn.close();
            }
        }
    }
}
