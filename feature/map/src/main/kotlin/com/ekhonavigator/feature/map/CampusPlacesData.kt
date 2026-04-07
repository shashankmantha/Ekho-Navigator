package com.ekhonavigator.feature.map

import com.google.android.gms.maps.model.LatLng

object CampusPlacesData {
    val places = listOf(
        // BUILDINGS
        CampusPlace(
            name = "Broome Library",
            position = LatLng(34.16269924567034, -119.04095431988378),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Main campus library and academic study space",
            quickPreviewSummary = "Study space, books, research help",
            studentVisitReasons = "Students go here to study, use library materials, reserve study rooms, and get research support.",
            keyServicesOffered = "Ask a Librarian support, study rooms, and equipment checkout.",
            studentProTip = "A good place to stop between classes if you need a quiet place to work or help finding academic sources.",
            campusOfficePhoneNumber = "Contact: 805-437-8561"
        ),
        CampusPlace(
            name = "Bell Tower",
            position = LatLng(34.16094392676536, -119.04308517583166),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Central campus building area with classrooms and student support offices",
            quickPreviewSummary = "Landmark, classes, student support",
            studentVisitReasons = "Students may go here for classes, academic advising, counseling, or other student support offices in the Bell Tower area.",
            keyServicesOffered = "Academic advising, Dean of Students support, CARE Team support, and counseling services in the Bell Tower/Bell Tower East area.",
            studentProTip = "Helpful as a central campus landmark and a starting point for several student support offices.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Sage Hall/Enrollment Center",  //There is conflicting info on the campus sites, saying one service was moved elsewhere, but another page says it's still in sage hall. holding for now
            position = LatLng(34.16409704291065, -119.04221707938154),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Student services",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Gateway Hall",
            position = LatLng(34.164959668539744, -119.04514675441855),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Academic and student services building near the campus entrance",
            quickPreviewSummary = "Welcome Center, classes, study spaces",
            studentVisitReasons = "Students may go here for classes, study spaces, the Welcome Center, or Enrollment Management services.",
            keyServicesOffered = "Welcome Center, Enrollment Management services, classrooms, study rooms, gathering spaces, and academic labs.",
            studentProTip = "Useful to know as a front-door campus building for both student services and academic spaces.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Marin Hall",
            position = LatLng(34.16448029307431, -119.04506628814714),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Student services and administrative building",
            quickPreviewSummary = "Records, billing, financial aid",
            studentVisitReasons = "Students may go here for registration and records help, student account and payment questions, or financial aid support.",
            keyServicesOffered = "Registrar's Office, Student Business Services, and Financial Aid services.",
            studentProTip = "A useful building to know for common enrollment, records, billing, and aid questions.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Modoc Hall",
            position = LatLng(34.16412297902208, -119.04835467640991),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Academic building with classrooms, labs, and conservation-related space",
            quickPreviewSummary = "Classes, labs, conservation office",
            studentVisitReasons = "Students may go here for classes, lab work, or conservation-related programs and office visits.",
            keyServicesOffered = "Classrooms, labs, and the Conservation Office.",
            studentProTip = "Useful to know if you have science-related classes or activities near the Biology Garden area.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "El Dorado Hall",
            position = LatLng(34.16420953349231, -119.04711549582302),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Academic building that houses the Graduate Studies Center",
            quickPreviewSummary = "Graduate studies, study spaces, meetings",
            studentVisitReasons = "Students may go here for graduate studies support, studying, meetings, or campus events tied to the Graduate Studies Center.",
            keyServicesOffered = "Graduate Studies Center support, study spaces, and meeting or conference room space.",
            studentProTip = "Useful to know if you are a graduate or credential student looking for study space or graduate support resources.",
            campusOfficePhoneNumber = "Contact: 805-437-3579"
        ),
        CampusPlace(
            name = "Napa Hall",
            position = LatLng(34.16369686340805, -119.04565369191049),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Academic arts building with studios, classrooms, and gallery space",
            quickPreviewSummary = "Art classes, studios, gallery space",
            studentVisitReasons = "Students may go here for art classes, studio work, gallery visits, or digital media and recording-related coursework.",
            keyServicesOffered = "Studio classrooms, a small computer lab, faculty and staff offices, the Napa Hall Art Gallery, and the Mike Curb Studio.",
            studentProTip = "Useful to know if you have art, photography, video, audio, or other creative coursework on campus.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Solano Hall",
            position = LatLng(34.163361739718496, -119.04515480103048),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Academic and support-services building with classrooms, nursing, and teaching innovation spaces",
            quickPreviewSummary = "Nursing, classrooms, teaching tech",
            studentVisitReasons = "Students may go here for classes, Nursing Program offices, or technology support in Solano Hall.",
            keyServicesOffered = "Nursing Program offices, the FIT Studio, the IT Solution Center, and classroom space.",
            studentProTip = "Useful to know if you have nursing-related needs or classes in the north quad area.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Del Norte Hall",
            position = LatLng(34.16314868025611, -119.04408191744362),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Academic building with classrooms, computer labs, and a lecture hall",
            quickPreviewSummary = "Lecture hall, classrooms, computer labs",
            studentVisitReasons = "Students may go here for lectures, regular classes, computer lab courses, or study time in the student lounge area.",
            keyServicesOffered = "Lecture hall, classrooms, computer labs, and a student lounge area.",
            studentProTip = "Good building to know if you have larger lecture classes or computer-based courses.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Madera Hall",
            position = LatLng(34.162933400884064, -119.04388075176304),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Academic and office building used by education programs and campus departments",
            quickPreviewSummary = "Education programs, offices, classrooms",
            studentVisitReasons = "Students may go here for School of Education programs, classes, or meetings with campus offices located in the building.",
            keyServicesOffered = "School of Education offices, Institutional Research, and classroom or lecture space.",
            studentProTip = "Useful to know if you are in education-related programs or have meetings in campus offices housed here.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Richard R. Rush Hall",
            position = LatLng(34.16262934556803, -119.04342209402576),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Administrative building that houses the Office of the President",
            quickPreviewSummary = "Administration, President's Office, collection",
            studentVisitReasons = "Students may go here for university administration-related visits or to access resources located in the building.",
            keyServicesOffered = "Office of the President and the Michele Serros Collection.",
            studentProTip = "Useful to know as a main administration building near the center of campus.",
            campusOfficePhoneNumber = "Contact: 805-437-8410"
        ),
        CampusPlace(
            name = "Sierra Hall",
            position = LatLng(34.16224317155607, -119.04461299484034),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Science and technology building with labs, classrooms, and collaboration spaces",
            quickPreviewSummary = "Science, technology, labs",
            studentVisitReasons = "Students may go here for science or technology classes, lab work, computer science spaces, or group collaboration.",
            keyServicesOffered = "Classrooms, computer and science labs, collaboration areas, and computer science program space.",
            studentProTip = "Useful to know if you have STEM classes or lab-based courses.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Ojai Hall",
            position = LatLng(34.161626177213485, -119.04252623619175),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Academic building with classrooms, offices, and computer labs",
            quickPreviewSummary = "Classes, offices, computer labs",
            studentVisitReasons = "Students may go here for classes, computer lab work, or visits to offices located in the building.",
            keyServicesOffered = "Classrooms, office space, and computer labs.",
            studentProTip = "Useful to know if you have computer-based coursework or classes near the center of campus.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Malibu Hall",
            position = LatLng(34.161212687793316, -119.04097989053598),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Performing arts building with instructional, rehearsal, and performance spaces",
            quickPreviewSummary = "Performing arts, dance, music",
            studentVisitReasons = "Students may go here for performing arts classes, rehearsals, performances, or meetings with faculty.",
            keyServicesOffered = "Recital and performance space, dance studio space, and performing arts faculty offices.",
            studentProTip = "Useful to know if you take dance, music, or theatre classes or attend campus performances.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Yuba Hall/Student Health Services",
            position = LatLng(34.16399732962278, -119.04108401027618),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Student health services building for basic medical care and wellness support",
            quickPreviewSummary = "Health care, appointments, wellness support",
            studentVisitReasons = "Students may go here for medical appointments, basic health care, physical exams, immunization-related help, or general wellness support.",
            keyServicesOffered = "Basic health care services, appointments, physical exams, immunization support, health education, and prescription-related support.",
            studentProTip = "Useful to know anytime you need non-emergency health support on campus.",
            campusOfficePhoneNumber = "Contact: 805-437-8828"
        ),
        CampusPlace(
            name = "Ironwood Hall",
            position = LatLng(34.162539342799676, -119.0464490267396),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Operations and support-services building for facilities, logistics, and campus services",
            quickPreviewSummary = "Facilities, mail, logistics support",
            studentVisitReasons = "Students may go here for mail-related needs or other campus service visits connected to offices in the building.",
            keyServicesOffered = "Facilities Services, the Work Center, the Mail Center, Shipping and Receiving, and Procurement and Contract Services.",
            studentProTip = "Mostly useful for campus service needs rather than regular classes.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Chaparral Hall",
            position = LatLng(34.162099441347955, -119.04566142665234),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Academic building",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Aliso Hall",
            position = LatLng(34.16090271825481, -119.04526673476065),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Science building with biology labs, lecture space, and program offices",
            quickPreviewSummary = "Biology, science labs, classes",
            studentVisitReasons = "Students may go here for biology and science classes, lab work, or visits related to the Biology and Natural Sciences Program.",
            keyServicesOffered = "Biology and Natural Sciences Program offices, science labs, and lecture space.",
            studentProTip = "Useful to know if you have science courses with lab sections.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Aliso Annex",
            position = LatLng(34.16112751725973, -119.04571329582699),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Academic building",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Trinity Hall",
            position = LatLng(34.15933133426799, -119.04242418314429),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Student-focused building space that includes the Esports Lounge",
            quickPreviewSummary = "Esports lounge, student space",
            studentVisitReasons = "Students may go here to use the Esports Lounge, join gaming events, or access student-use space in the building.",
            keyServicesOffered = "Esports Lounge access and a designated lactation room.",
            studentProTip = "Useful to know if you are involved in campus gaming or looking for the Esports Lounge.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Topanga Hall",
            position = LatLng(34.16011480443447, -119.04166109466685),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Art building with sculpture, ceramics, digital media, and fabrication spaces",
            quickPreviewSummary = "Sculpture, ceramics, digital media",
            studentVisitReasons = "Students may go here for art classes, studio work, ceramics, sculpture, digital media, or fabrication projects.",
            keyServicesOffered = "Sculpture studio space, ceramics facilities, the Topanga Computer Lab, FabLab tools, and Art Program support space.",
            studentProTip = "Useful to know if you take studio art classes that need hands-on work, specialized tools, or digital production space.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Lindero Hall",
            position = LatLng(34.159513335398444, -119.04140349327288),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Administrative building with business, budget, and human resources offices",
            quickPreviewSummary = "HR, budget, admin offices",
            studentVisitReasons = "Students may go here for student employment or other visits connected to administrative offices housed in the building.",
            keyServicesOffered = "Human Resources, Budget and Planning, the Vice President's Office for Business and Financial Affairs, Internal Audit, and University Glen Home Sales.",
            studentProTip = "Mostly useful for administrative or employment-related needs rather than regular classes.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Carden Kids Preschool",
            position = LatLng(34.16477247708901, -119.04348799190726),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "On-campus preschool and childcare center",
            quickPreviewSummary = "Preschool, childcare, family support",
            studentVisitReasons = "Students with children may go here for preschool or childcare information for young children.",
            keyServicesOffered = "On-campus preschool services and childcare information.",
            studentProTip = "Useful to know if you are balancing school and childcare needs.",
            campusOfficePhoneNumber = "Contact: 805-482-6386"
        ),
        CampusPlace(
            name = "Shasta Hall",
            position = LatLng(34.1645461051814, -119.04471912587594),
            category = PlaceCategory.BUILDINGS,
            fullLocationDescription = "Academic support building with Extended University and International Programs offices",
            quickPreviewSummary = "Extended University, international programs",
            studentVisitReasons = "Students may go here for Open University or Extended University help, continuing education support, or International Programs services.",
            keyServicesOffered = "Extended University, Open University support, and International Programs offices.",
            studentProTip = "Useful to know if you need help with international programs or courses taken through Extended University.",
            campusOfficePhoneNumber = null
        ),

        // SERVICES / FACILITIES
        CampusPlace(
            name = "Town Center",
            position = LatLng(34.163144356251436, -119.0392759941097),
            category = PlaceCategory.SERVICES,
            fullLocationDescription = "Residential and retail hub with student apartments, bookstore, and eateries",
            quickPreviewSummary = "Housing, bookstore, eateries",
            studentVisitReasons = "Students may go here to live on campus, buy bookstore items, or grab food from Town Center dining spots.",
            keyServicesOffered = "Student apartments, The Cove Bookstore, and dining options.",
            studentProTip = "Useful stop on the east side of campus for food or bookstore needs.",
            campusOfficePhoneNumber = null
        ),
        CampusPlace(
            name = "Grand Salon",
            position = LatLng(34.1639111458964, -119.04361178503132),
            category = PlaceCategory.SERVICES,
            fullLocationDescription = "Large campus event venue in the North Quad",
            quickPreviewSummary = "Ceremonies, receptions, large events",
            studentVisitReasons = "Students may go here for ceremonies, campus programs, receptions, or other large gatherings.",
            keyServicesOffered = "Large event space with theater and banquet layouts, a front foyer, nearby restrooms, climate control, and wireless access.",
            studentProTip = "Useful to know as one of the main campus venues for major events and celebrations.",
            campusOfficePhoneNumber = "University Events Contact: 805-437-3900"
        ),
        CampusPlace(
            name = "Conference Center",
            position = LatLng(34.163783533053795, -119.04353131875963),
            category = PlaceCategory.SERVICES,
            fullLocationDescription = "Event space",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Petit Salon",
            position = LatLng(34.16360598443106, -119.04349108562353),
            category = PlaceCategory.SERVICES,
            fullLocationDescription = "Campus event space in the North Quad",
            quickPreviewSummary = "Events, celebrations, receptions",
            studentVisitReasons = "Students may go here for student programs, affinity celebrations, meetings, or receptions.",
            keyServicesOffered = "Reservable event space used for campus programs, celebrations, and gatherings.",
            studentProTip = "Useful to know as a common indoor venue for smaller campus events than Grand Salon.",
            campusOfficePhoneNumber = "University Events Contact: 805-437-3900"
        ),
        CampusPlace(
            name = "Central Plant",
            position = LatLng(34.16319532044837, -119.04694979280103),
            category = PlaceCategory.SERVICES,
            fullLocationDescription = "Campus utility facility that supports heating, cooling, and infrastructure operations",
            quickPreviewSummary = "Utilities, heating, cooling support",
            studentVisitReasons = "Most students would not visit this facility unless they work with Facilities Services or related campus operations.",
            keyServicesOffered = "Support for campus utility operations and the university's main heating and cooling systems.",
            studentProTip = "Mostly a behind-the-scenes operations site rather than a regular student destination.",
            campusOfficePhoneNumber = "Contact: 805-437-3545"
        ),
        CampusPlace(
            name = "Facilities Services Work Center",
            position = LatLng(34.162736939394556, -119.04573841191757),
            category = PlaceCategory.SERVICES,
            fullLocationDescription = "Facilities service request and maintenance coordination office",
            quickPreviewSummary = "Maintenance help, keys, work orders",
            studentVisitReasons = "Students may contact or visit this office for maintenance issues, key or code requests, HVAC or lighting problems, and custodial or grounds concerns.",
            keyServicesOffered = "Work order processing, key and code requests, maintenance coordination, and Facilities Services support.",
            studentProTip = "Useful for reporting campus facility problems rather than for regular class visits.",
            campusOfficePhoneNumber = "Contact: 805-437-8461"
        ),
        CampusPlace(
            name = "Maintenance Warehouse Stores",
            position = LatLng(34.16265661627372, -119.04718433873771),
            category = PlaceCategory.SERVICES,
            fullLocationDescription = "Facilities support store for materials and equipment",
            quickPreviewSummary = "Materials, equipment, facilities support",
            studentVisitReasons = "Students usually would not visit unless coordinating approved facilities or operational needs.",
            keyServicesOffered = "Orders, stores, tracks, and delivers materials and equipment for Facilities Services shops.",
            studentProTip = "Mostly a behind-the-scenes operations site, not a regular student destination.",
            campusOfficePhoneNumber = "Contact: 805-437-8468"
        ),
        CampusPlace(
            name = "Paint Shop",
            position = LatLng(34.16145026844382, -119.04600332655171),
            category = PlaceCategory.SERVICES,
            fullLocationDescription = "Facilities operations shop for interior and exterior campus painting",
            quickPreviewSummary = "Campus painting and upkeep",
            studentVisitReasons = "Students usually would not visit unless they work with Facilities Services or related campus operations.",
            keyServicesOffered = "Interior and exterior painting support for campus buildings.",
            studentProTip = "Mostly a behind-the-scenes operations site rather than a regular student destination.",
            campusOfficePhoneNumber = "Contact: 805-437-3223"
        ),
        CampusPlace(
            name = "CSUCI University Police (Placer Hall)",
            position = LatLng(34.163303339253346, -119.04298830751196),
            category = PlaceCategory.SERVICES,
            fullLocationDescription = "Campus facility",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Sage Hall/Enrollment Center",
            position = LatLng(34.16409704291065, -119.04221707938154),
            category = PlaceCategory.SERVICES,
            fullLocationDescription = "Student services",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Yuba Hall/Student Health Services",
            position = LatLng(34.16399732962278, -119.04108401027618),
            category = PlaceCategory.SERVICES,
            fullLocationDescription = "Academic building",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),

        // HOUSING
        CampusPlace(
            name = "Arroyo Hall",
            position = LatLng(34.16038050274691, -119.04497067609698),
            category = PlaceCategory.HOUSING,
            fullLocationDescription = "Residence Hall",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Anacapa Village Commons Building",
            position = LatLng(34.159145244639355, -119.04489939214349),
            category = PlaceCategory.HOUSING,
            fullLocationDescription = "Housing",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Anacapa Village A",
            position = LatLng(34.159158561500966, -119.04449035527075),
            category = PlaceCategory.HOUSING,
            fullLocationDescription = "Housing",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Anacapa Village B",
            position = LatLng(34.1597589279052, -119.04470761420434),
            category = PlaceCategory.HOUSING,
            fullLocationDescription = "Housing",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Anacapa Village C",
            position = LatLng(34.15960578493917, -119.04532988669999),
            category = PlaceCategory.HOUSING,
            fullLocationDescription = "Housing",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Santa Cruz Village D",
            position = LatLng(34.160115479056195, -119.04413373409251),
            category = PlaceCategory.HOUSING,
            fullLocationDescription = "Housing",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Santa Cruz Village E",
            position = LatLng(34.159944581088965, -119.04408545432916),
            category = PlaceCategory.HOUSING,
            fullLocationDescription = "Housing",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Santa Cruz Village F",
            position = LatLng(34.159825840089866, -119.0440009647433),
            category = PlaceCategory.HOUSING,
            fullLocationDescription = "Housing",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Santa Cruz Village G",
            position = LatLng(34.1595850283848, -119.04384271441177),
            category = PlaceCategory.HOUSING,
            fullLocationDescription = "Housing",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Santa Cruz Village H",
            position = LatLng(34.159287619441514, -119.04378102360305),
            category = PlaceCategory.HOUSING,
            fullLocationDescription = "Housing",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Santa Rosa Village",
            position = LatLng(34.1590279837229, -119.04252312670616),
            category = PlaceCategory.HOUSING,
            fullLocationDescription = "Housing",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),

        // FOOD
        CampusPlace(
            name = "Islands Cafe",
            position = LatLng(34.16040443642445, -119.04154959797783),
            category = PlaceCategory.FOOD,
            fullLocationDescription = "Main Dining Hall",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Dolphin Food Pantry",
            position = LatLng(34.160584870736926, -119.04516127687491),
            category = PlaceCategory.FOOD,
            fullLocationDescription = "Student Food Assistance",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),

        // PARKING
        CampusPlace(
            name = "Parking Lot A1",
            position = LatLng(34.1615574298747, -119.0422449606771),
            category = PlaceCategory.PARKING,
            fullLocationDescription = "Student parking",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Parking Lot A2",
            position = LatLng(34.16417724914477, -119.04179625023635),
            category = PlaceCategory.PARKING,
            fullLocationDescription = "Student parking",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Parking Lot A3",
            position = LatLng(34.16657143937711, -119.04704616885896),
            category = PlaceCategory.PARKING,
            fullLocationDescription = "Permit parking",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Parking Lot A4",
            position = LatLng(34.164183316315004, -119.04645632740116),
            category = PlaceCategory.PARKING,
            fullLocationDescription = "Permit parking",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Parking Lot A5",
            position = LatLng(34.16028738317786, -119.04457875614888),
            category = PlaceCategory.PARKING,
            fullLocationDescription = "Permit parking",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Parking Lot A6",
            position = LatLng(34.16322198914701, -119.04202689595482),
            category = PlaceCategory.PARKING,
            fullLocationDescription = "Permit parking",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Parking Lot A7",
            position = LatLng(34.1606339735571, -119.04119526733172),
            category = PlaceCategory.PARKING,
            fullLocationDescription = "Permit parking",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Parking Lot A8",
            position = LatLng(34.16302567637596, -119.04031330802815),
            category = PlaceCategory.PARKING,
            fullLocationDescription = "Permit parking",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Parking Lot R",
            position = LatLng(34.16291456374539, -119.04317367769761),
            category = PlaceCategory.PARKING,
            fullLocationDescription = "Permit parking",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Parking Lot A/E",
            position = LatLng(34.16181596432445, -119.0415536234366),
            category = PlaceCategory.PARKING,
            fullLocationDescription = "Permit parking",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Parking Lot A10",
            position = LatLng(34.15929656149958, -119.04060022214941),
            category = PlaceCategory.PARKING,
            fullLocationDescription = "Permit parking",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Parking Lot A11",
            position = LatLng(34.16445171167112, -119.04799616089558),
            category = PlaceCategory.PARKING,
            fullLocationDescription = "Permit parking",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Parking Lot SH2",
            position = LatLng(34.15834261398384, -119.0410768256611),
            category = PlaceCategory.PARKING,
            fullLocationDescription = "Permit parking",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "Parking Lot SH1",
            position = LatLng(34.159024110922225, -119.04536405524816),
            category = PlaceCategory.PARKING,
            fullLocationDescription = "Permit parking",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),

        // GENERAL
        CampusPlace(
            name = "North Quad",
            position = LatLng(34.163753423241644, -119.04437064818602),
            category = PlaceCategory.ALL,
            fullLocationDescription = "Quad area",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
        CampusPlace(
            name = "South Quad",
            position = LatLng(34.160127068848, -119.04267310796122),
            category = PlaceCategory.ALL,
            fullLocationDescription = "Quad area",
            quickPreviewSummary = "[Add one-line preview here]",
            studentVisitReasons = "[Add reasons here]",
            keyServicesOffered = "[Add services here]",
            studentProTip = "[Add tip here]",
            campusOfficePhoneNumber = "[Add phone number here or null]"
        ),
    )
}
