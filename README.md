# CS200Project-
Semester Long Project for CS 200 
ChocAn Data Processing System - Project 4
==========================================

COMPILE (from this folder):
    javac *.java

RUN (graphical version - recommended):
    java ChocAnGUI

RUN (command-line version, same data and rules):
    java MainMenu

THE GUI
    Look and feel: a soft aqua background, white rounded cards with gentle
    shadows, sky-blue buttons, and a sidebar on the left. After you log in, the
    sidebar lists the pages for your role (click one to open it), shows who is
    logged in, and has the Log out button. The small number tiles at the top of
    the pages (members, fees, Acme changes, ...) update by themselves.
    1. Choose Provider, Operator, or Manager and type your 9-digit number. The
       number is checked against provider.txt / operator.txt / manager.txt.
       "Show sample logins" lists the numbers you can use.
    2. Provider screen: Validate Member, Bill ChocAn (member, date, service code,
       confirm the service name, comments - then the verification form with the
       fee appears), and Provider Directory (table + save to file).
    3. Operator screen: Members and Providers tables with Search, Add, Edit
       (double-click a row works too), and Delete. A form shows which field is
       wrong if a save is refused. After each change the line is read back from
       the file and shown at the bottom of the tab.
    4. Manager screen: Reports (member, provider, summary, EFT, or the whole
       accounting procedure, with a preview of each file), This Week's
       Services, and Acme Updates (the acmesimulation.txt schedule).
    5. In the background the GUI checks every 10 seconds whether the weekly
       update is due and whether an Acme change has come due.
    6. The login screen has a red "Force apply the Acme change right now" button
       (no login needed). It applies every waiting line of acmesimulation.txt
       immediately. The same button is on the Manager > Acme Updates tab, and in
       the command-line version it is role-menu option 4 / manager option 6.

The "data" folder already contains pregenerated fake test data. Reports and
the Provider Directory are written when you run the program:
    ProviderDirectory/            the Provider Directory file (provider menu option 3)
    reports/members/              one file per member        (manager option 1)
    reports/providers/            one file per provider      (manager option 2)
    reports/summary/              Accounts Payable Summary   (manager option 3)
    reports/eft/                  EFT data file              (manager option 4)
    (manager option 5 writes all of them at once)

RESET THE TEST DATA (do this after testing, or if "this week" has no records):
    java SampleData
This rewrites every file in "data". The billed services in records.txt are
dated relative to the day you reset, so reset again if a week has passed.

LOGIN NUMBERS (sample data)
    Provider  (data/provider.txt)  000012345 Druid City Nutrition
                                   000054321 Northport Medical Group
                                   000099999 Crimson Fitness Center
                                   000011111 Magic City Wellness
                                   000022222 Hoover Health & Fitness
              (a provider is a hospital or clinic, so the name is the hospital/clinic name)
    Operator  (data/operator.txt)  000250000  000250001
    Manager   (data/manager.txt)   000150000  000150001

SAMPLE MEMBERS (data/member.txt)
    Active:    123456789 Alice Brown, 555123456 Cara Diaz, 111222333, 444555666, 222333444, 333444555
    Suspended: 987654321 Ben Carter, 777888999 Frank Moore

SAMPLE SERVICE CODES (data/services.txt)
    598470 Dietitian session ($85.00)    883948 Aerobics exercise ($45.50)
    201114 Internist checkup ($120.00)   (10 services in total)

CLASS DIAGRAMS (docs folder)
    ChocAn_ClassDiagram.uxf          both diagrams in one file; open it in UMLet to edit
    ChocAn_ClassDiagram_1_core.png   diagram 1 of 2: the core system
    ChocAn_ClassDiagram_2_gui.png    diagram 2 of 2: the graphical user interface
    ChocAn_ClassDiagram.pdf          both diagrams, one per page
    Every class is coloured by the team member who built it (legend at the bottom).
    Every class, attribute, and method drawn was checked against the source code.

NAMING: Party instead of Person
    Member and Provider both extend the abstract class Party: the name, nine-digit
    number, and mailing address that ChocAn keeps for anyone it deals with. A member
    is a person and a provider is a hospital or clinic, so Party is the honest name.
    Operators and managers are not parties; they are only login numbers in
    operator.txt and manager.txt.

WHO BUILT WHAT
    Sydney Renninger  MainMenu, Terminal, ProviderTerminal, OperatorTerminal,
                      ManagerTerminal, LoginController, and the whole GUI
                      (ChocAnGUI, AppPanel, LoginPanel, ProviderPanel, OperatorPanel,
                      ManagerPanel, Ui)
    Florencia         Party, Member, Provider
    Namita            Timer (the weekly update), ReportController, Report,
                      SummaryReport, EFTReport, Service, ServiceRecord
    Sameeksha         AcmeInterface, MemberController, ProviderController,
                      ServiceController, ProviderDirectory, MemberReport,
                      ProviderReport, SampleData, AppFolders, and the data-file
                      persistence in ChocAnDatabase
    Samantha          ChocAnDatabase (storage and lookups)

DATA FILES (data folder)
    provider.txt   hospital/clinic name|number|address|city|state|zip|type - also the provider login list
    operator.txt   one number per line                       - operator login list
    manager.txt    one number per line                       - manager login list
    member.txt     name|number|address|city|state|zip|status
    services.txt   code|name|fee                             - Provider Directory
    records.txt    one line per billed service, exactly the fields in the project description:
                   date+time received|date of service|provider #|member #|service code|comments
                   (service name and fee are looked up in services.txt)
    acmesimulation.txt  memberNumber,status,MM-DD-YYYY HH:MM:SS - simulated Acme file

ACME SIMULATION (data/acmesimulation.txt)
    Each line is memberNumber,status,time. Every time the role menu shows, the
    program checks the current date/time and applies each line whose time has
    passed, then marks it ",APPLIED" so it never runs twice.
    Sample file: Ben Carter (987654321) -> Active and Cara Diaz (555123456) ->
    Suspended are already due, so they change the first time the role menu
    appears (you will see "[Acme] 2 membership status update(s) applied.").
    Frank Moore (777888999) -> Active is scheduled for tomorrow 9 PM.
    To test live: add a line with a time one minute from now, then keep
    pressing Enter / choosing an invalid option at the role menu until
    the minute passes. Manager menu option 6 applies every pending line
    immediately, ignoring the times.

THE WEEKLY UPDATE AND "THIS WEEK'S DATA"
    "This week" = service records received in the last 7 days (records.txt).
    Every Friday at 12:00 AM the weekly update runs by itself:
        1. the main accounting procedure (all reports, built from this week's data)
        2. then a NEW WEEK starts: this week's services are moved to
           records_archive.txt (nothing is deleted) so the next reports start clean.
    The time of the last update is saved in weekly_update.txt, so it never runs
    twice for the same Friday. If the program was closed at Friday midnight, the
    update runs the next time the program is opened or checked. (The very first
    start only creates weekly_update.txt; nothing runs.)
    The manager can clear this week's data by hand at any time:
        GUI: Manager > This Week's Services > "Clear this week's data"
        CLI: Manager menu option 7
    "Run main accounting procedure" (Manager > Reports, or CLI option 5) builds
    all reports but does NOT clear the data.
    Run "java SampleData" to reset everything, including weekly_update.txt and
    records_archive.txt.

WHERE THE FILES ARE
    The program prints "Data folder: <full path>" every time it starts. That is
    the folder it reads and writes, no matter where you start it from (command
    line or IDE). If you are looking at a different "data" folder, you will not
    see the changes. The reports and ProviderDirectory folders are created next
    to the data folder.

REAL-TIME SYNC WITH THE FILES (both directions)
    Program -> file: every change is written immediately. The program opens
    member.txt / provider.txt, finds the line with that number, and replaces it
    (update), removes it (delete), or adds it (add); other lines are untouched.
    Billed services are appended to records.txt. After each write the file is
    read back to check the change is really there. The confirmation shows the
    full path, e.g. "Member added. Saved in /.../data/member.txt". If the file
    After every operator change the program also reads that person's line back
    out of the file and prints it, e.g.
        Member updated. Saved in /.../data/member.txt
          In the file now -> line 9 of 9: New Guy|999888777|1 A St|Birmingham|AL|35401|Active
    Operator menu options 7 and 8 print member.txt / provider.txt exactly as
    they are on disk, with the full path.
    If the file cannot be written (locked, read-only) you get "Not saved: Could not write ..."
    and nothing changes.
    File -> program: if you edit a data file by hand while the program is
    running (member.txt, provider.txt, records.txt, services.txt, operator.txt,
    manager.txt), the program re-reads it the next time it needs the data.
    Lines that are not valid (bad number, 2-letter state, status, ...) are
    skipped and the program prints a WARNING that says which line and why.
    Updating a member or provider asks which part to change (name, address,
    city, state, ZIP, status/type); the number itself cannot be changed.

The Summary and EFT reports only include providers that are still in the system.

"This week" = service records received in the last 7 days.
The Timer runs the weekly reports at Friday 12 AM; managers can also run
everything by hand (Manager menu option 5).
