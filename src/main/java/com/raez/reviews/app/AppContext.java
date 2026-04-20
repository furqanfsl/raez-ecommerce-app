package com.raez.reviews.app;

import java.nio.file.Path;

import com.raez.reviews.dao.AdminDao;
import com.raez.reviews.dao.CustomerDao;
import com.raez.reviews.dao.ModerationAuditDao;
import com.raez.reviews.dao.OrderDao;
import com.raez.reviews.dao.ProductDao;
import com.raez.reviews.dao.ReviewDao;
import com.raez.reviews.dao.SettingsDao;
import com.raez.reviews.dao.VoteDao;
import com.raez.reviews.service.AuthService;
import com.raez.reviews.service.EligibilityService;
import com.raez.reviews.service.ModerationService;
import com.raez.reviews.service.ReviewService;
import com.raez.reviews.service.SettingsService;
import com.raez.reviews.service.VoteService;
import com.raez.reviews.util.DatabaseInitializer;
import com.raez.reviews.util.DatabaseManager;

public class AppContext {

    private static AppContext INSTANCE;

    private final DatabaseManager databaseManager;
    private final AuthService authService;
    private final EligibilityService eligibilityService;
    private final SettingsService settingsService;
    private final ReviewService reviewService;
    private final VoteService voteService;
    private final ModerationService moderationService;

    public AppContext(Path databasePath) {
        this.databaseManager = new DatabaseManager(databasePath);
        new DatabaseInitializer(databaseManager).initialize();

        CustomerDao customerDao        = new CustomerDao(databaseManager);
        AdminDao adminDao              = new AdminDao(databaseManager);
        ProductDao productDao          = new ProductDao(databaseManager);
        OrderDao orderDao              = new OrderDao(databaseManager);
        ReviewDao reviewDao            = new ReviewDao(databaseManager);
        VoteDao voteDao                = new VoteDao(databaseManager);
        SettingsDao settingsDao        = new SettingsDao(databaseManager);
        ModerationAuditDao auditDao    = new ModerationAuditDao(databaseManager);

        this.authService         = new AuthService(customerDao, adminDao);
        this.eligibilityService  = new EligibilityService(orderDao, reviewDao);
        this.settingsService     = new SettingsService(settingsDao);
        this.reviewService       = new ReviewService(databaseManager, productDao, reviewDao, eligibilityService, settingsService);
        this.voteService         = new VoteService(databaseManager, reviewDao, voteDao);
        this.moderationService   = new ModerationService(databaseManager, reviewDao, auditDao);
    }

    /** Lazily creates and caches the singleton (path is ignored after first call). */
    public static AppContext getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AppContext(Path.of("raez.db"));
        }
        return INSTANCE;
    }

    public AuthService        getAuthService()        { return authService; }
    public EligibilityService getEligibilityService() { return eligibilityService; }
    public SettingsService    getSettingsService()    { return settingsService; }
    public ReviewService      getReviewService()      { return reviewService; }
    public VoteService        getVoteService()        { return voteService; }
    public ModerationService  getModerationService()  { return moderationService; }
    public DatabaseManager    getDatabaseManager()    { return databaseManager; }
}
