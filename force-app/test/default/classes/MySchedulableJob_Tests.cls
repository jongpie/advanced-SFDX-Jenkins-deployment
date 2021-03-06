@isTest
private class MySchedulableJob_Tests {
    private static String CRON_EXP_DAILY_AT_6 = '0 0 6 * * ?';

    @isTest
    private static void it_should_schedule_job() {
        Test.startTest();

        Id jobId = System.schedule('TestMySchedulableJob', CRON_EXP_DAILY_AT_6, new MySchedulableJob());

        // Get the information from the CronTrigger API object
        CronTrigger cronTrigger = [SELECT Id, CronExpression, TimesTriggered, NextFireTime FROM CronTrigger WHERE id = :jobId];

        // Verify the expressions are the same
        System.assertEquals(CRON_EXP_DAILY_AT_6, cronTrigger.CronExpression);

        // Verify the job has not run
        System.assertEquals(0, cronTrigger.TimesTriggered);

        // Verify the next time the job will run
        String expectedDatetime = String.valueOf(System.today().addDays(1)) + ' 06:00:00';
        System.assertEquals(expectedDatetime, String.valueOf(cronTrigger.NextFireTime));

        Test.stopTest();
    }
}
