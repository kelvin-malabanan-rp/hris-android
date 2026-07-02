package io.rocketpartners.hris.core.networking.mock

/**
 * Canned `data`-payload JSON for each route, mirroring the iOS `MockURLProtocol` fixtures
 * (synthetic demo data, ~mid-2026). [dataPayload] returns the inner `data` value (or `"null"` for
 * void endpoints); [MockInterceptor] wraps it in the real `ApiResponse` envelope.
 *
 * Raw JSON strings use `\"` for embedded quotes — valid JSON, and Kotlin raw strings leave the
 * backslash intact. None contain `$` so there is no accidental string interpolation.
 */
internal object MockFixtures {

    fun dataPayload(method: String, path: String): String = when {
        method == "POST" && path.endsWith("/auth/login") -> loginData
        method == "POST" && path.endsWith("/auth/refresh") -> tokensData
        method == "GET" && path.endsWith("/auth/me") -> userData
        method == "POST" && path.endsWith("/auth/logout") -> "null"

        method == "GET" && path.endsWith("/calendar/events") -> eventsData
        method == "GET" && path.endsWith("/calendar/users-on-leave") -> usersOnLeaveData

        method == "GET" && path.endsWith("/leave-applications/balances/my") -> balancesData
        method == "GET" && path.endsWith("/leave-applications/my") -> leavesPagedData
        method == "GET" && path.endsWith("/leave-applications/pending-approvals") -> leavePendingApprovalsData
        method == "GET" && path.endsWith("/leave-types/active") -> leaveTypesData
        method == "POST" && path.contains("/leave-applications/") && path.endsWith("/cancel") ->
            singleLeave("CANCELLED", "Cancelled")
        method == "POST" && path.contains("/leave-applications/") && path.endsWith("/request-cancellation") ->
            singleLeave("APPROVED", "Cancellation Requested")
        method == "POST" && path.contains("/leave-applications/") && path.endsWith("/cancellation/approve") ->
            singleLeave("CANCELLED", "Cancelled")
        method == "POST" && path.contains("/leave-applications/") && path.endsWith("/cancellation/reject") ->
            singleLeave("APPROVED", "Approved")
        method == "POST" && path.contains("/leave-applications/") && path.endsWith("/approve") ->
            singleLeave("APPROVED", "Approved")
        method == "POST" && path.contains("/leave-applications/") && path.endsWith("/reject") ->
            singleLeave("REJECTED", "Rejected")
        method == "PUT" && path.contains("/leave-applications/") ->
            singleLeave("PENDING_MANAGER", "Pending Manager")
        method == "POST" && path.endsWith("/leave-applications") ->
            singleLeave("PENDING_MANAGER", "Pending Manager")

        method == "GET" && path.endsWith("/wfh/weekly-usage") -> wfhUsageData
        method == "GET" && path.endsWith("/wfh/pending-approvals") -> wfhPendingApprovalsData
        method == "POST" && path.contains("/wfh/schedules/") && path.endsWith("/cancel") -> wfhCancelData
        method == "POST" && path.contains("/wfh/schedules/") && path.endsWith("/approve") ->
            wfhReviewedData("approved")
        method == "POST" && path.contains("/wfh/schedules/") && path.endsWith("/reject") ->
            wfhReviewedData("rejected")
        method == "GET" && path.endsWith("/wfh/schedules") -> wfhSchedulesData
        method == "POST" && path.endsWith("/wfh/schedules") -> wfhNewScheduleData

        method == "GET" && path.endsWith("/notifications/unread-count") -> unreadCountData
        method == "PATCH" && path.endsWith("/notifications/read-all") -> "null"
        method == "PATCH" && path.contains("/notifications/") && path.endsWith("/read") -> singleNotification
        method == "POST" && path.endsWith("/notifications/devices") -> deviceData
        method == "DELETE" && path.endsWith("/notifications/devices") -> "null"
        method == "GET" && path.endsWith("/notifications") -> notificationsPagedData

        method == "GET" && path.endsWith("/payslips/me") -> payslipsData
        method == "GET" && path.endsWith("/asset-assignments/my-assets") -> myAssetsData

        method == "POST" && path.contains("/tickets/") && path.endsWith("/messages") -> ticketReplyData
        method == "GET" && path.contains("/tickets/") -> ticketDetailData
        method == "POST" && path.endsWith("/tickets") -> ticketCreatedData
        method == "GET" && path.endsWith("/tickets") -> ticketsPagedData

        method == "GET" && path.contains("/announcements/") -> announcementDetailData
        method == "GET" && path.endsWith("/announcements") -> announcementsData

        method == "PUT" && path.endsWith("/users/me/password") -> "null"
        path.endsWith("/users/me") -> profileData

        else -> "null"
    }

    private fun singleLeave(status: String, label: String): String =
        """{"id":1006,"leaveTypeId":1,"leaveTypeName":"Annual Leave","leaveTypeColor":"#0A84FF","startDate":"2026-07-10","endDate":"2026-07-11","totalDays":2.0,"reason":"Requested via app","status":"$status","statusLabel":"$label"}"""

    private fun wfhReviewedData(status: String): String =
        """{"id":5001,"date":"2026-07-06","dayName":"Monday","type":"one-time","reason":"Plumber visit, working from home","status":"$status","userId":7,"userName":"Maria Santos"}"""

    private val userData =
        """{"id":42,"name":"Angelo Soliveres","email":"angelo.soliveres@rocketpartners.io","permissions":["WFH_READ","WFH_CREATE","WFH_APPROVE"]}"""

    private val loginData =
        """{"accessToken":"mock-access-token","refreshToken":"mock-refresh-token","user":{"id":42,"name":"Angelo Soliveres","email":"angelo.soliveres@rocketpartners.io"}}"""

    private val tokensData =
        """{"accessToken":"mock-access-token-refreshed","refreshToken":"mock-refresh-token"}"""

    private val wfhPendingApprovalsData = """
        [
          {"id":5001,"date":"2026-07-06","dayName":"Monday","type":"one-time","reason":"Plumber visit, working from home","status":"pending","userId":7,"userName":"Maria Santos","managerComments":null},
          {"id":5002,"date":"2026-07-08","dayName":"Wednesday","type":"one-time","reason":"Focus day for the release","status":"pending","userId":12,"userName":"James Park","managerComments":null}
        ]
    """.trimIndent()

    private val profileData =
        """{"id":42,"email":"angelo.soliveres@rocketpartners.io","firstName":"Angelo","lastName":"Soliveres","fullName":"Angelo Soliveres","phone":"+63 917 555 0142","personalEmail":"angelo.personal@example.com","employeeId":"RP-0042","departmentName":"Engineering","positionTitle":"Senior iOS Engineer","profileImageUrl":null,"address":"123 Ayala Avenue","addressLine2":"Unit 14B","city":"Makati","state":"Metro Manila","postalCode":"1226","country":"Philippines","emergencyContactName":"Maria Soliveres","emergencyContactRelationship":"Spouse","emergencyContactPhone":"+63 2 8555 0100","emergencyContactMobile":"+63 917 555 0199"}"""

    private val eventsData = """
        [
          {"id":"evt-1","title":"Sprint Planning","start":"2026-06-15T09:00:00Z","end":"2026-06-15T10:30:00Z","allDay":false,"color":"#0A84FF","type":"meeting"},
          {"id":"evt-2","title":"Independence Day — Company Holiday","start":"2026-06-12","end":"2026-06-12","allDay":true,"color":"#FF453A","type":"holiday"},
          {"id":"evt-3","title":"Design Review","start":"2026-06-17T14:00:00Z","end":"2026-06-17T15:00:00Z","allDay":false,"color":"#BF5AF2","type":"meeting"},
          {"id":"evt-4","title":"1:1 with Manager","start":"2026-06-18T11:00:00Z","end":"2026-06-18T11:30:00Z","allDay":false,"color":"#30D158","type":"meeting"},
          {"id":"evt-5","title":"Team Offsite","start":"2026-06-24","end":"2026-06-25","allDay":true,"color":"#FF9F0A","type":"event"},
          {"id":"evt-6","title":"Release Cutoff","start":"2026-06-30T17:00:00Z","end":"2026-06-30T17:00:00Z","allDay":false,"color":"#0A84FF","type":"deadline"}
        ]
    """.trimIndent()

    private val usersOnLeaveData = """
        [
          {"id":901,"user":{"id":7,"name":"Maria Santos","avatar":null,"department":{"id":1,"name":"Engineering"}},"leaveType":{"id":1,"name":"Annual Leave","color":"#0A84FF"},"startDate":"2026-06-13","endDate":"2026-06-16","totalDays":2.0},
          {"id":902,"user":{"id":12,"name":"James Park","avatar":null,"department":{"id":2,"name":"Design"}},"leaveType":{"id":2,"name":"Sick Leave","color":"#FF9F0A"},"startDate":"2026-06-13","endDate":"2026-06-13","totalDays":1.0}
        ]
    """.trimIndent()

    private val leavesPagedData = """
        {"content":[
          {"id":1001,"leaveTypeId":1,"leaveTypeName":"Annual Leave","leaveTypeColor":"#0A84FF","startDate":"2026-06-22","endDate":"2026-06-24","totalDays":3.0,"reason":"Family trip","status":"PENDING_MANAGER","statusLabel":"Pending Manager"},
          {"id":1002,"leaveTypeId":2,"leaveTypeName":"Sick Leave","leaveTypeColor":"#FF9F0A","startDate":"2026-06-05","endDate":"2026-06-05","totalDays":1.0,"reason":"Flu","status":"APPROVED","statusLabel":"Approved"},
          {"id":1003,"leaveTypeId":1,"leaveTypeName":"Annual Leave","leaveTypeColor":"#0A84FF","startDate":"2026-05-19","endDate":"2026-05-20","totalDays":2.0,"reason":"Personal matters","status":"REJECTED","statusLabel":"Rejected"},
          {"id":1004,"leaveTypeId":3,"leaveTypeName":"Emergency Leave","leaveTypeColor":"#FF453A","startDate":"2026-04-10","endDate":"2026-04-10","totalDays":1.0,"reason":"Family emergency","status":"CANCELLED","statusLabel":"Cancelled"},
          {"id":1005,"leaveTypeId":1,"leaveTypeName":"Annual Leave","leaveTypeColor":"#0A84FF","startDate":"2026-07-01","endDate":"2026-07-03","totalDays":3.0,"reason":"Vacation","status":"PENDING_HR","statusLabel":"Pending HR"}
        ]}
    """.trimIndent()

    private val leavePendingApprovalsData = """
        {"page":{"content":[
          {"id":3001,"leaveTypeId":1,"leaveTypeName":"Annual Leave","leaveTypeColor":"#0A84FF","startDate":"2026-07-13","endDate":"2026-07-15","totalDays":3.0,"reason":"Family vacation","status":"PENDING_MANAGER","statusLabel":"Pending Manager","userName":"Maria Santos"},
          {"id":3002,"leaveTypeId":2,"leaveTypeName":"Sick Leave","leaveTypeColor":"#FF9F0A","startDate":"2026-07-09","endDate":"2026-07-09","totalDays":1.0,"reason":"Doctor's appointment","status":"PENDING_MANAGER","statusLabel":"Pending Manager","userName":"James Park"},
          {"id":3003,"leaveTypeId":1,"leaveTypeName":"Annual Leave","leaveTypeColor":"#0A84FF","startDate":"2026-07-20","endDate":"2026-07-21","totalDays":2.0,"reason":"Plans changed","status":"PENDING_CANCELLATION","statusLabel":"Pending Cancellation","userName":"Aisha Rahman"}
        ],"totalElements":3,"empty":false},"summary":{"pendingManager":2,"pendingHr":0,"pendingCancellation":1}}
    """.trimIndent()

    private val balancesData = """
        [
          {"id":1,"leaveTypeId":1,"leaveTypeName":"Annual Leave","year":2026,"totalDays":15.0,"usedDays":4.0,"pendingDays":3.0,"remainingDays":8.0},
          {"id":2,"leaveTypeId":2,"leaveTypeName":"Sick Leave","year":2026,"totalDays":10.0,"usedDays":1.0,"pendingDays":0.0,"remainingDays":9.0},
          {"id":3,"leaveTypeId":3,"leaveTypeName":"Emergency Leave","year":2026,"totalDays":5.0,"usedDays":1.0,"pendingDays":0.0,"remainingDays":4.0},
          {"id":4,"leaveTypeId":4,"leaveTypeName":"Unpaid Leave","year":2026,"totalDays":0.0,"usedDays":0.0,"pendingDays":0.0,"remainingDays":0.0}
        ]
    """.trimIndent()

    private val leaveTypesData = """
        [
          {"id":1,"name":"Annual Leave","code":"AL","color":"#0A84FF"},
          {"id":2,"name":"Sick Leave","code":"SL","color":"#FF9F0A","requiresMedicalCert":true,"medicalCertDaysThreshold":2},
          {"id":3,"name":"Emergency Leave","code":"EL","color":"#FF453A"},
          {"id":4,"name":"Unpaid Leave","code":"UL","color":"#8E8E93"}
        ]
    """.trimIndent()

    private val wfhSchedulesData = """
        [
          {"id":2001,"date":"2026-06-09","dayName":"Monday","type":"WFH","reason":"Focus day","status":"APPROVED"},
          {"id":2002,"date":"2026-06-11","dayName":"Wednesday","type":"WFH","reason":null,"status":"APPROVED"},
          {"id":2003,"date":"2026-06-16","dayName":"Monday","type":"WFH","reason":"Deliveries","status":"PENDING"},
          {"id":2004,"date":"2026-06-23","dayName":"Monday","type":"WFH","reason":null,"status":"PENDING"}
        ]
    """.trimIndent()

    private val wfhNewScheduleData = """
        [
          {"id":2005,"date":"2026-06-30","dayName":"Tuesday","type":"WFH","reason":"Requested via app","status":"APPROVED"},
          {"id":2006,"date":"2026-07-01","dayName":"Wednesday","type":"WFH","reason":"Requested via app","status":"PENDING"}
        ]
    """.trimIndent()

    private val wfhCancelData =
        """{"id":2003,"date":"2026-06-16","dayName":"Monday","type":"WFH","reason":"Deliveries","status":"CANCELLED"}"""

    private val wfhUsageData = """{"used":2,"quota":3,"remaining":1}"""

    // Timestamps are generated as offsets from now so the inbox's time sections (Today / Yesterday /
    // Last 7 days / Earlier) never decay — mirrors the iOS fixture regeneration.
    private fun agoIso(daysAgo: Long, hoursAgo: Long): String =
        java.time.format.DateTimeFormatter.ISO_INSTANT.format(
            java.time.Instant.now()
                .minus(java.time.Duration.ofDays(daysAgo).plusHours(hoursAgo))
                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS),
        )

    private val notificationsPagedData: String
        get() = """
        {"content":[
          {"id":5001,"type":"LEAVE_APPROVED","title":"Leave approved","message":"Your annual leave for June 22-24 was approved by your manager.","referenceType":"LEAVE","referenceId":1001,"isRead":false,"readAt":null,"createdAt":"${agoIso(0, 2)}"},
          {"id":5002,"type":"LEAVE_REQUESTED","title":"Leave request submitted","message":"Your leave request is awaiting manager approval.","referenceType":"LEAVE","referenceId":1005,"isRead":false,"readAt":null,"createdAt":"${agoIso(0, 5)}"},
          {"id":5003,"type":"TICKET_REPLY","title":"New reply on your ticket","message":"IT replied to your hardware request ticket.","referenceType":"TICKET","referenceId":42,"isRead":false,"readAt":null,"createdAt":"${agoIso(1, 3)}"},
          {"id":5004,"type":"LEAVE_REJECTED","title":"Leave rejected","message":"Your leave for May 19-20 was rejected.","referenceType":"LEAVE","referenceId":1003,"isRead":true,"readAt":null,"createdAt":"${agoIso(3, 0)}"},
          {"id":5005,"type":"ONBOARDING_APPROVED","title":"Onboarding approved","message":"Your onboarding documents were approved. Welcome aboard!","referenceType":"ONBOARDING","referenceId":7,"isRead":true,"readAt":null,"createdAt":"${agoIso(20, 0)}"}
        ],"totalElements":5,"empty":false}
    """.trimIndent()

    private val unreadCountData = """{"count":3}"""

    private val singleNotification =
        """{"id":5001,"type":"LEAVE_APPROVED","title":"Leave approved","message":"Your annual leave was approved.","referenceType":"LEAVE","referenceId":1001,"isRead":true,"readAt":"2026-06-16T09:00:00Z","createdAt":"2026-06-16T08:30:00Z"}"""

    private val deviceData =
        """{"id":1,"token":"mock-device-token","platform":"ANDROID","environment":"sandbox"}"""

    private val payslipsData = """
        {"content":[
          {"id":9001,"payPeriodId":201,"payPeriodLabel":"June 2026","cutoffDate":"2026-06-30","fileName":"payslip-2026-06.pdf","contentType":"application/pdf","fileSize":48213,"source":"GENERATED","createdAt":"2026-07-01T09:00:00"},
          {"id":9002,"payPeriodId":200,"payPeriodLabel":"May 2026","cutoffDate":"2026-05-31","fileName":"payslip-2026-05.pdf","contentType":"application/pdf","fileSize":47980,"source":"GENERATED","createdAt":"2026-06-01T09:00:00"},
          {"id":9003,"payPeriodId":199,"payPeriodLabel":"April 2026","cutoffDate":"2026-04-30","fileName":"payslip-2026-04.pdf","contentType":"application/pdf","fileSize":47551,"source":"UPLOAD","createdAt":"2026-05-01T09:00:00"}
        ],"last":true,"totalElements":3,"empty":false}
    """.trimIndent()

    private val myAssetsData = """
        [
          {"id":7001,"assetId":300,"assetName":"MacBook Pro 16-inch","assetTag":"RP-LT-0042","categoryName":"Laptop","quantityAssigned":1,"checkedOutAt":"2026-01-15T09:00:00","expectedReturnDate":"2026-12-31","conditionOnCheckout":"GOOD","assignedByName":"IT Helpdesk","checkoutNotes":"Primary work laptop. Return on contract end.","status":"CHECKED_OUT"},
          {"id":7002,"assetId":311,"assetName":"Dell UltraSharp Monitor","assetTag":"RP-MN-0188","categoryName":"Monitor","quantityAssigned":1,"checkedOutAt":"2026-02-01T10:30:00","expectedReturnDate":"2026-07-02","conditionOnCheckout":"NEW","assignedByName":"IT Helpdesk","status":"CHECKED_OUT"}
        ]
    """.trimIndent()

    private val ticketsPagedData = """
        {"content":[
          {"id":42,"subject":"Laptop won't boot","description":"Black screen since this morning.","category":"bug","priority":"high","status":"in_progress","userId":42,"userName":"Angelo Soliveres","messageCount":2,"resolvedAt":null,"createdAt":"2026-06-15T09:00:00Z","updatedAt":"2026-06-16T10:00:00Z"},
          {"id":43,"subject":"Request a second monitor","description":"Could I get a 27-inch display for the home setup?","category":"question","priority":"low","status":"open","userId":42,"userName":"Angelo Soliveres","messageCount":1,"resolvedAt":null,"createdAt":"2026-06-12T14:00:00Z","updatedAt":"2026-06-12T14:00:00Z"},
          {"id":44,"subject":"VPN keeps disconnecting","description":"Drops every few minutes on the office network.","category":"bug","priority":"medium","status":"resolved","userId":42,"userName":"Angelo Soliveres","messageCount":3,"resolvedAt":"2026-06-10T16:00:00Z","createdAt":"2026-06-08T09:30:00Z","updatedAt":"2026-06-10T16:00:00Z"}
        ],"last":true,"totalElements":3,"empty":false}
    """.trimIndent()

    private val ticketDetailData = """
        {"id":42,"subject":"Laptop won't boot","description":"Black screen since this morning.","category":"bug","priority":"high","status":"in_progress","userId":42,"userName":"Angelo Soliveres","resolvedAt":null,"createdAt":"2026-06-15T09:00:00Z","updatedAt":"2026-06-16T10:00:00Z","messages":[
          {"id":1,"userId":42,"userName":"Angelo Soliveres","userProfilePicture":null,"isSupport":false,"message":"My MacBook won't turn on - just a black screen after the chime.","attachments":[],"createdAt":"2026-06-15T09:00:00Z"},
          {"id":2,"userId":7,"userName":"IT Helpdesk","userProfilePicture":null,"isSupport":true,"message":"Thanks for reporting. Can you try holding the power button for 10 seconds, then booting again?","attachments":[{"id":501,"fileName":"reset-guide.pdf","storedPath":"/files/reset-guide.pdf","contentType":"application/pdf","fileSize":20480,"downloadUrl":"/files/reset-guide.pdf","createdAt":"2026-06-16T10:00:00Z"}],"createdAt":"2026-06-16T10:00:00Z"}
        ],"attachments":[
          {"id":500,"fileName":"black-screen.jpg","storedPath":"/files/black-screen.jpg","contentType":"image/jpeg","fileSize":138240,"downloadUrl":"/files/black-screen.jpg","createdAt":"2026-06-15T09:00:00Z"}
        ]}
    """.trimIndent()

    private val ticketCreatedData =
        """{"id":45,"subject":"New ticket from app","description":"Submitted via the mobile app.","category":"other","priority":"medium","status":"open","userId":42,"userName":"Angelo Soliveres","messageCount":1,"resolvedAt":null,"createdAt":"2026-06-28T09:00:00Z","updatedAt":"2026-06-28T09:00:00Z"}"""

    private val ticketReplyData =
        """{"id":3,"userId":42,"userName":"Angelo Soliveres","userProfilePicture":null,"isSupport":false,"message":"Tried that - still a black screen.","attachments":[],"createdAt":"2026-06-28T09:05:00Z"}"""

    private val announcementsData = """
        {"content":[
          {"id":8001,"title":"Q3 All-Hands Meeting","body":"<p>Join us for the <strong>Q3 all-hands</strong> this Friday at 3pm in the main hall.</p>","category":"COMPANY_NEWS","pinned":true,"authorId":1,"authorName":"People Team","authorPosition":"HR","authorImageUrl":null,"publishedAt":"2026-06-25T09:00:00","images":[],"commentsCount":4,"createdAt":"2026-06-25T09:00:00"},
          {"id":8002,"title":"New Wellness Benefits","body":"<p>We've expanded our wellness program.</p>","category":"HR_UPDATES","pinned":false,"authorId":1,"authorName":"People Team","authorPosition":"HR","authorImageUrl":null,"publishedAt":"2026-06-20T10:30:00","images":[{"id":9001,"url":"/uploads/images/wellness.jpg","fileName":"wellness.jpg","sortOrder":0}],"commentsCount":1,"createdAt":"2026-06-20T10:30:00"},
          {"id":8003,"title":"Friday Game Night","body":"<p>Board games &amp; pizza in the lounge at 5pm.</p>","category":"FUN","pinned":false,"authorId":12,"authorName":"Social Committee","authorPosition":null,"authorImageUrl":null,"publishedAt":"2026-06-18T16:00:00","images":[],"commentsCount":0,"createdAt":"2026-06-18T16:00:00"}
        ],"last":true,"totalElements":3,"empty":false}
    """.trimIndent()

    private val announcementDetailData = """
        {"id":8001,"title":"Q3 All-Hands Meeting","body":"<h2>Agenda</h2><p>Join us for the <strong>Q3 all-hands</strong> this Friday at 3pm.</p><ul><li>Company updates</li><li>Q&amp;A</li></ul>","category":"COMPANY_NEWS","pinned":true,"authorId":1,"authorName":"People Team","authorPosition":"HR","authorImageUrl":null,"publishedAt":"2026-06-25T09:00:00","images":[{"id":9001,"url":"/uploads/images/allhands.jpg","fileName":"allhands.jpg","sortOrder":0}],"reactions":{"thumbs_up":12,"heart":5},"userReactions":["heart"],"commentsCount":4,"comments":[],"createdAt":"2026-06-25T09:00:00"}
    """.trimIndent()
}
