package isla.dao;

class InMemoryDaoTest extends DaoContractTest {

    @Override
    protected void createDaos() {
        items = new InMemoryItemDAO();
        players = new InMemoryPlayerDAO(items);
        vendors = new InMemoryVendorDAO(items);
        quests = new InMemoryQuestDAO();
    }
}
