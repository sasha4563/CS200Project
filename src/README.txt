ChocAn Data Processing System - Project 4
==========================================

COMPILE (from this folder):
    javac *.java

RUN:
    java MainMenu

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
