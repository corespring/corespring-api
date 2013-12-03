function up() {

    var NEW_CLASSROOMS_COLLECTION_ID = "51df104fe4b073dbbb1c84fa";
    var newClassroomsContent = db.content.find({"collectionId": NEW_CLASSROOMS_COLLECTION_ID});

    var noMatchCount = 0;

    // This comes from mapping a New Classrooms CSV file against the standards we seed to our database
    var skillNumberMap = {};
    skillNumberMap['101'] = ["5.NBT.A.2", "5.NBT.A.1"];
    skillNumberMap['102'] = ["7.SP.C.8a", "7.SP.C.5"];
    skillNumberMap['103'] = [];
    skillNumberMap['104'] = [];
    skillNumberMap['105'] = ["6.SP.A.2", "6.SP.A.3", "6.SP.B.5b"];
    skillNumberMap['106'] = ["7.NS.A.2a", "7.NS.A.2b", "7.NS.A.2c", "7.NS.A.3"];
    skillNumberMap['107'] = ["5.NF.B.4a", "5.NF.B.5a", "5.NF.B.5b", "5.NF.B.6"];
    skillNumberMap['108'] = ["4.OA.C.5"];
    skillNumberMap['109'] = ["4.NF.B.3b", "4.NF.B.3a"];
    skillNumberMap['110'] = ["4.NF.A.2"];
    skillNumberMap['111'] = ["8.EE.A.1"];
    skillNumberMap['112'] = ["5.MD.C.5a", "5.MD.C.3a", "5.MD.C.3b", "5.MD.C.4", "5.MD.C.5b"];
    skillNumberMap['113'] = [];
    skillNumberMap['114'] = ["8.G.A.2"];
    skillNumberMap['115'] = ["4.NBT.A.1", "3.NBT.A.3"];
    skillNumberMap['116'] = ["6.EE.A.2c"];
    skillNumberMap['117'] = ["6.RP.A.3a"];
    skillNumberMap['118'] = [];
    skillNumberMap['119'] = ["7.SP.C.7a", "7.SP.C.5"];
    skillNumberMap['120'] = ["6.G.A.1"];
    skillNumberMap['121'] = [];
    skillNumberMap['122'] = ["6.G.A.4"];
    skillNumberMap['123'] = ["6.SP.B.4"];
    skillNumberMap['124'] = ["5.G.A.2"];
    skillNumberMap['125'] = [];
    skillNumberMap['126'] = ["4.NF.A.1"];
    skillNumberMap['127'] = [];
    skillNumberMap['128'] = [];
    skillNumberMap['129'] = ["8.F.B.5", "HSF-IF.B.4"];
    skillNumberMap['130'] = [];
    skillNumberMap['131'] = [];
    skillNumberMap['132'] = [];
    skillNumberMap['133'] = ["2.NBT.B.5", "2.OA.A.1"];
    skillNumberMap['134'] = [];
    skillNumberMap['135'] = [];
    skillNumberMap['136'] = [];
    skillNumberMap['137'] = [];
    skillNumberMap['138'] = ["6.EE.A.1"];
    skillNumberMap['139'] = ["7.EE.A.1", "7.EE.A.2"];
    skillNumberMap['140'] = ["6.SP.B.4"];
    skillNumberMap['141'] = ["6.RP.A.3c"];
    skillNumberMap['142'] = ["4.NBT.A.3", "4.NBT.A.1"];
    skillNumberMap['143'] = [];
    skillNumberMap['144'] = ["7.EE.B.4b"];
    skillNumberMap['145'] = ["8.F.B.4", "HSF-LE.B.5"];
    skillNumberMap['146'] = ["8.F.A.3", "HSF-LE.A.1"];
    skillNumberMap['147'] = ["3.NF.A.1", "3.NF.A.2"];
    skillNumberMap['148'] = [];
    skillNumberMap['149'] = ["3.OA.A.3", "3.OA.A.4", "3.OA.C.7"];
    skillNumberMap['150'] = ["8.SP.A.2", "8.SP.A.3", "HSS-ID.B.6c", "HSS-ID.C.7"];
    skillNumberMap['151'] = [];
    skillNumberMap['152'] = ["7.NS.A.1c", "7.NS.A.1d"];
    skillNumberMap['153'] = ["3.MD.D.8", "4.MD.A.3"];
    skillNumberMap['154'] = ["7.G.A.1"];
    skillNumberMap['155'] = [];
    skillNumberMap['156'] = ["7.SP.B.3", "7.SP.B.4"];
    skillNumberMap['157'] = [];
    skillNumberMap['158'] = [];
    skillNumberMap['159'] = [];
    skillNumberMap['160'] = [];
    skillNumberMap['161'] = ["7.RP.A.2d", "7.RP.A.2b"];
    skillNumberMap['162'] = [];
    skillNumberMap['163'] = [];
    skillNumberMap['164'] = [];
    skillNumberMap['165'] = [];
    skillNumberMap['166'] = ["7.SP.C.8b"];
    skillNumberMap['167'] = ["8.F.A.1", "HSA-CED.A.2", "HSA-REI.D.10"];
    skillNumberMap['168'] = [];
    skillNumberMap['169'] = ["4.NBT.A.2", "4.NBT.A.1"];
    skillNumberMap['170'] = ["6.NS.B.4"];
    skillNumberMap['171'] = ["4.NF.B.3d", "4.NF.B.3a"];
    skillNumberMap['172'] = ["4.G.A.1", "4.MD.C.5a", "4.MD.C.5b", "4.MD.C.6"];
    skillNumberMap['173'] = [];
    skillNumberMap['174'] = ["7.G.B.4"];
    skillNumberMap['175'] = [];
    skillNumberMap['176'] = ["5.NBT.A.3b", "5.NBT.A.1"];
    skillNumberMap['177'] = ["6.EE.B.8", "6.EE.B.5"];
    skillNumberMap['178'] = ["8.EE.A.2"];
    skillNumberMap['179'] = ["7.EE.B.4a"];
    skillNumberMap['180'] = [];
    skillNumberMap['181'] = ["7.G.B.6"];
    skillNumberMap['182'] = ["7.NS.A.1b", "7.NS.A.1d"];
    skillNumberMap['183'] = ["4.G.A.1"];
    skillNumberMap['184'] = ["5.G.B.3", "5.G.B.4", "4.G.A.2"];
    skillNumberMap['185'] = ["3.MD.C.7c", "3.OA.B.5"];
    skillNumberMap['186'] = ["6.NS.B.3"];
    skillNumberMap['187'] = ["5.NF.B.6", "5.NF.B.4a", "5.NF.B.5b"];
    skillNumberMap['188'] = ["8.F.A.1"];
    skillNumberMap['189'] = [];
    skillNumberMap['190'] = [];
    skillNumberMap['191'] = [];
    skillNumberMap['192'] = ["5.NBT.B.5"];
    skillNumberMap['193'] = [];
    skillNumberMap['194'] = ["8.G.A.3"];
    skillNumberMap['195'] = [];
    skillNumberMap['196'] = ["5.NBT.A.4", "5.NBT.A.1"];
    skillNumberMap['197'] = [];
    skillNumberMap['198'] = ["7.G.B.4"];
    skillNumberMap['199'] = ["7.SP.A.1"];
    skillNumberMap['200'] = ["HSA-REI.B.4b", "HSA-SSE.B.3a"];
    skillNumberMap['201'] = [];
    skillNumberMap['202'] = [];
    skillNumberMap['203'] = ["6.EE.A.2c"];
    skillNumberMap['204'] = ["8.EE.C.7b", "HSA-REI.A.1", "HSA-REI.B.3"];
    skillNumberMap['205'] = ["8.G.A.2"];
    skillNumberMap['206'] = [];
    skillNumberMap['207'] = [];
    skillNumberMap['208'] = [];
    skillNumberMap['209'] = ["6.NS.C.6b", "6.NS.C.6c", "6.NS.C.6a", "6.NS.C.8"];
    skillNumberMap['210'] = ["4.NBT.B.4"];
    skillNumberMap['211'] = ["7.G.B.5"];
    skillNumberMap['212'] = ["3.OA.B.5", "1.OA.B.3"];
    skillNumberMap['213'] = ["6.RP.A.1"];
    skillNumberMap['214'] = [];
    skillNumberMap['215'] = [];
    skillNumberMap['216'] = ["6.NS.B.3"];
    skillNumberMap['217'] = [];
    skillNumberMap['218'] = [];
    skillNumberMap['219'] = ["7.SP.C.6", "7.SP.C.7b", "7.SP.C.5"];
    skillNumberMap['220'] = ["4.OA.B.4"];
    skillNumberMap['221'] = ["8.G.B.7"];
    skillNumberMap['222'] = ["6.EE.B.5", "6.EE.B.7"];
    skillNumberMap['223'] = ["5.MD.A.1"];
    skillNumberMap['224'] = ["8.F.B.4", "HSA-CED.A.2", "HSF-BF.A.1a", "HSF-LE.A.2"];
    skillNumberMap['225'] = ["6.SP.B.4"];
    skillNumberMap['226'] = ["6.RP.A.2"];
    skillNumberMap['227'] = [];
    skillNumberMap['228'] = ["6.EE.C.9", "6.RP.A.3b"];
    skillNumberMap['229'] = [];
    skillNumberMap['230'] = ["7.RP.A.2a"];
    skillNumberMap['231'] = ["7.NS.A.2d", "7.EE.B.3"];
    skillNumberMap['232'] = ["8.F.A.2", "HSF-IF.C.9"];
    skillNumberMap['233'] = ["6.NS.B.3"];
    skillNumberMap['234'] = [];
    skillNumberMap['235'] = [];
    skillNumberMap['236'] = ["8.EE.C.8a", "HSA-REI.C.6"];
    skillNumberMap['237'] = ["HSA-APR.A.1"];
    skillNumberMap['238'] = ["4.OA.B.4"];
    skillNumberMap['239'] = [];
    skillNumberMap['240'] = ["8.EE.C.7b", "HSA-REI.A.1", "HSA-REI.B.3"];
    skillNumberMap['241'] = [];
    skillNumberMap['242'] = ["8.G.A.3"];
    skillNumberMap['243'] = ["7.RP.A.3"];
    skillNumberMap['244'] = [];
    skillNumberMap['245'] = ["6.G.A.3", "6.NS.C.6b"];
    skillNumberMap['246'] = [];
    skillNumberMap['247'] = ["6.NS.B.4"];
    skillNumberMap['248'] = ["3.MD.C.7a", "3.MD.C.7b"];
    skillNumberMap['249'] = ["8.G.A.5"];
    skillNumberMap['250'] = ["7.EE.B.3"];
    skillNumberMap['251'] = [];
    skillNumberMap['252'] = [];
    skillNumberMap['253'] = [];
    skillNumberMap['254'] = [];
    skillNumberMap['255'] = [];
    skillNumberMap['256'] = [];
    skillNumberMap['257'] = ["5.NF.A.1", "5.NF.A.2"];
    skillNumberMap['258'] = [];
    skillNumberMap['259'] = ["HSA-APR.A.1"];
    skillNumberMap['260'] = [];
    skillNumberMap['261'] = ["8.G.B.6"];
    skillNumberMap['262'] = ["8.G.A.1"];
    skillNumberMap['263'] = ["8.NS.A.2"];
    skillNumberMap['264'] = ["6.G.A.1"];
    skillNumberMap['265'] = ["6.EE.A.1"];
    skillNumberMap['266'] = [];
    skillNumberMap['267'] = ["HSA-APR.A.1"];
    skillNumberMap['268'] = [];
    skillNumberMap['269'] = [];
    skillNumberMap['270'] = ["8.EE.A.4"];
    skillNumberMap['271'] = ["7.G.A.1", "7.G.B.6"];
    skillNumberMap['272'] = ["3.OA.A.1"];
    skillNumberMap['273'] = ["HSA-REI.A.1", "HSA-REI.B.3"];
    skillNumberMap['274'] = ["5.OA.A.1"];
    skillNumberMap['275'] = ["8.G.B.7"];
    skillNumberMap['276'] = [];
    skillNumberMap['277'] = ["5.NBT.A.3a"];
    skillNumberMap['278'] = [];
    skillNumberMap['279'] = ["7.EE.B.4a"];
    skillNumberMap['280'] = ["8.EE.A.4"];
    skillNumberMap['281'] = [];
    skillNumberMap['282'] = ["5.NBT.B.6"];
    skillNumberMap['283'] = ["5.NF.A.1", "5.NF.A.2"];
    skillNumberMap['284'] = ["8.G.A.3"];
    skillNumberMap['285'] = ["HSA-REI.B.4b", "HSA-SSE.B.3a"];
    skillNumberMap['286'] = [];
    skillNumberMap['287'] = ["3.MD.D.8"];
    skillNumberMap['288'] = ["4.NBT.B.5"];
    skillNumberMap['289'] = [];
    skillNumberMap['290'] = ["6.EE.A.2a", "6.EE.B.6", "6.EE.A.2b"];
    skillNumberMap['291'] = [];
    skillNumberMap['292'] = ["6.RP.A.3c"];
    skillNumberMap['293'] = ["6.SP.B.5b"];
    skillNumberMap['294'] = ["7.EE.B.4a"];
    skillNumberMap['295'] = ["4.NF.C.6", "4.NF.C.5"];
    skillNumberMap['296'] = [];
    skillNumberMap['297'] = [];
    skillNumberMap['298'] = [];
    skillNumberMap['299'] = ["7.RP.A.3"];
    skillNumberMap['300'] = ["HSA-CED.A.1", "HSA-SSE.A.1", "HSA-SSE.B.3c", "HSF-LE.A.2"];
    skillNumberMap['301'] = ["5.G.B.4", "4.G.A.2"];
    skillNumberMap['302'] = ["6.SP.B.5d", "6.SP.A.2", "6.SP.A.3"];
    skillNumberMap['303'] = [];
    skillNumberMap['304'] = ["6.NS.C.7c"];
    skillNumberMap['305'] = [];
    skillNumberMap['306'] = [];
    skillNumberMap['307'] = [];
    skillNumberMap['308'] = ["8.G.B.8"];
    skillNumberMap['309'] = [];
    skillNumberMap['310'] = ["8.NS.A.1"];
    skillNumberMap['311'] = [];
    skillNumberMap['312'] = [];
    skillNumberMap['313'] = ["8.EE.A.2", "8.NS.A.2", "HSA-REI.B.4b"];
    skillNumberMap['314'] = [];
    skillNumberMap['315'] = ["7.G.A.3"];
    skillNumberMap['316'] = ["8.G.A.5"];
    skillNumberMap['317'] = ["6.RP.A.3d"];
    skillNumberMap['318'] = ["6.NS.A.1"];
    skillNumberMap['319'] = ["HSF-IF.B.4", "HSF-IF.C.7a"];
    skillNumberMap['320'] = [];
    skillNumberMap['321'] = [];
    skillNumberMap['322'] = ["8.F.B.4", "HSF-LE.A.2"];
    skillNumberMap['323'] = ["HSA-REI.D.10"];
    skillNumberMap['324'] = ["HSA-REI.D.11", "HSA-REI.C.7"];
    skillNumberMap['325'] = ["8.F.A.1"];
    skillNumberMap['326'] = ["HSA-CED.A.2"];
    skillNumberMap['327'] = [];
    skillNumberMap['328'] = [];
    skillNumberMap['329'] = ["HSF-IF.C.8"];
    skillNumberMap['330'] = [];
    skillNumberMap['331'] = ["HSA-CED.A.1"];
    skillNumberMap['332'] = ["8.EE.C.8c", "HSA-CED.A.3", "HSA-REI.C.6"];
    skillNumberMap['333'] = ["HSA-CED.A.1", "HSF-IF.C.8a"];
    skillNumberMap['334'] = ["HSA-CED.A.1", "HSA-CED.A.2", "HSA-SSE.A.1", "HSF-IF.B.5", "HSF-LE.A.1", "HSF-LE.A.2", "HSF-LE.B.5"];
    skillNumberMap['335'] = ["8.EE.C.8b", "HSA-REI.C.6"];
    skillNumberMap['336'] = ["HSA-REI.D.11", "HSA-REI.C.7"];
    skillNumberMap['337'] = ["HSA-APR.A.1"];
    skillNumberMap['338'] = [];
    skillNumberMap['339'] = [];
    skillNumberMap['340'] = [];
    skillNumberMap['341'] = [];
    skillNumberMap['342'] = ["HSA-SSE.A.2", "HSA-SSE.B.3a"];
    skillNumberMap['343'] = ["HSA-SSE.A.1", "HSA-SSE.B.3a"];
    skillNumberMap['344'] = [];
    skillNumberMap['345'] = ["HSA-CED.A.4", "HSA-REI.A.1"];
    skillNumberMap['346'] = ["HSA-CED.A.1", "HSA-REI.A.1", "HSA-REI.B.3"];
    skillNumberMap['347'] = [];
    skillNumberMap['348'] = [];
    skillNumberMap['349'] = ["HSA-REI.B.4b", "HSF-IF.C.8a"];
    skillNumberMap['350'] = [];
    skillNumberMap['351'] = [];
    skillNumberMap['352'] = ["HSF-IF.C.8", "HSF-IF.C.9"];
    skillNumberMap['353'] = [];
    skillNumberMap['354'] = [];
    skillNumberMap['355'] = [];
    skillNumberMap['356'] = [];
    skillNumberMap['357'] = [];
    skillNumberMap['358'] = ["HSA-CED.A.1", "HSA-REI.A.1", "HSA-REI.B.3"];
    skillNumberMap['359'] = [];
    skillNumberMap['360'] = [];
    skillNumberMap['361'] = [];
    skillNumberMap['362'] = [];
    skillNumberMap['363'] = ["HSA-CED.A.3", "HSA-REI.D.12"];
    skillNumberMap['364'] = ["HSA-CED.A.3", "HSA-REI.D.12"];
    skillNumberMap['365'] = ["HSA-REI.D.10", "HSF-IF.C.7e", "HSF-LE.A.3"];
    skillNumberMap['366'] = ["HSF-BF.B.3"];
    skillNumberMap['367'] = ["HSF-BF.B.3"];
    skillNumberMap['368'] = [];
    skillNumberMap['369'] = [];
    skillNumberMap['370'] = [];
    skillNumberMap['371'] = ["HSN-RN.A.1", "HSN-RN.A.2"];
    skillNumberMap['372'] = ["HSN-RN.A.1", "HSN-RN.A.2"];
    skillNumberMap['373'] = [];
    skillNumberMap['374'] = [];
    skillNumberMap['375'] = [];
    skillNumberMap['376'] = [];
    skillNumberMap['377'] = [];
    skillNumberMap['378'] = [];
    skillNumberMap['379'] = [];
    skillNumberMap['380'] = [];
    skillNumberMap['381'] = ["HSS-ID.C.9"];
    skillNumberMap['382'] = [];
    skillNumberMap['383'] = [];
    skillNumberMap['384'] = ["HSS-ID.A.1"];
    skillNumberMap['385'] = ["HSS-ID.A.1"];
    skillNumberMap['386'] = [];
    skillNumberMap['387'] = ["HSS-ID.A.2", "HSS-ID.A.3"];
    skillNumberMap['388'] = [];
    skillNumberMap['389'] = ["7.EE.B.4a"];
    skillNumberMap['390'] = ["4.NBT.B.6"];
    skillNumberMap['391'] = ["5.NF.B.4a", "5.NF.B.5b", "5.NF.B.5a"];
    skillNumberMap['393'] = [];
    skillNumberMap['394'] = [];
    skillNumberMap['395'] = [];
    skillNumberMap['396'] = [];
    skillNumberMap['397'] = [];
    skillNumberMap['398'] = [];
    skillNumberMap['399'] = [];
    skillNumberMap['400'] = [];
    skillNumberMap['401'] = [];
    skillNumberMap['402'] = [];
    skillNumberMap['403'] = [];
    skillNumberMap['404'] = [];
    skillNumberMap['405'] = [];
    skillNumberMap['410'] = [];
    skillNumberMap['412'] = [];
    skillNumberMap['413'] = [];
    skillNumberMap['414'] = [];
    skillNumberMap['416'] = [];
    skillNumberMap['417'] = [];
    skillNumberMap['418'] = [];
    skillNumberMap['419'] = [];
    skillNumberMap['420'] = [];
    skillNumberMap['421'] = [];
    skillNumberMap['422'] = [];
    skillNumberMap['423'] = [];
    skillNumberMap['424'] = [];
    skillNumberMap['425'] = [];
    skillNumberMap['426'] = [];
    skillNumberMap['427'] = [];
    skillNumberMap['428'] = [];
    skillNumberMap['429'] = [];
    skillNumberMap['432'] = [];
    skillNumberMap['433'] = [];
    skillNumberMap['434'] = [];
    skillNumberMap['435'] = [];
    skillNumberMap['436'] = [];
    skillNumberMap['437'] = [];
    skillNumberMap['438'] = [];
    skillNumberMap['439'] = [];
    skillNumberMap['440'] = [];
    skillNumberMap['441'] = [];
    skillNumberMap['442'] = [];
    skillNumberMap['443'] = [];
    skillNumberMap['444'] = ["8.G.C.9"];
    skillNumberMap['445'] = [];
    skillNumberMap['447'] = [];
    skillNumberMap['448'] = [];
    skillNumberMap['449'] = [];
    skillNumberMap['450'] = [];
    skillNumberMap['451'] = [];
    skillNumberMap['452'] = [];
    skillNumberMap['453'] = [];
    skillNumberMap['454'] = [];
    skillNumberMap['455'] = [];
    skillNumberMap['456'] = [];
    skillNumberMap['457'] = [];
    skillNumberMap['458'] = [];
    skillNumberMap['459'] = [];
    skillNumberMap['460'] = [];
    skillNumberMap['461'] = [];
    skillNumberMap['462'] = [];
    skillNumberMap['463'] = [];
    skillNumberMap['464'] = [];
    skillNumberMap['465'] = [];
    skillNumberMap['466'] = [];
    skillNumberMap['467'] = [];
    skillNumberMap['468'] = [];
    skillNumberMap['469'] = [];
    skillNumberMap['470'] = [];
    skillNumberMap['471'] = [];
    skillNumberMap['472'] = [];
    skillNumberMap['473'] = [];
    skillNumberMap['474'] = [];
    skillNumberMap['475'] = [];
    skillNumberMap['476'] = [];
    skillNumberMap['477'] = [];
    skillNumberMap['478'] = ["6.EE.A.3", "6.EE.A.4"];
    skillNumberMap['479'] = [];
    skillNumberMap['480'] = [];
    skillNumberMap['481'] = [];
    skillNumberMap['482'] = [];
    skillNumberMap['484'] = [];
    skillNumberMap['485'] = [];
    skillNumberMap['486'] = [];
    skillNumberMap['487'] = ["6.G.A.2"];
    skillNumberMap['488'] = [];
    skillNumberMap['489'] = [];
    skillNumberMap['490'] = [];
    skillNumberMap['491'] = [];
    skillNumberMap['492'] = [];
    skillNumberMap['493'] = [];
    skillNumberMap['494'] = [];
    skillNumberMap['495'] = [];
    skillNumberMap['496'] = [];
    skillNumberMap['497'] = [];
    skillNumberMap['499'] = ["6.NS.C.5", "6.NS.C.6a", "6.NS.C.6c", "6.NS.C.7a", "6.NS.C.7b"];
    skillNumberMap['500'] = ["7.G.A.2"];
    skillNumberMap['501'] = ["7.RP.A.1"];
    skillNumberMap['502'] = ["7.SP.A.2"];
    skillNumberMap['503'] = ["8.SP.A.4"];
    skillNumberMap['504'] = ["5.MD.B.2"];
    skillNumberMap['505'] = ["5.NF.B.7a", "5.NF.B.7c"];
    skillNumberMap['506'] = ["5.NF.B.7b", "5.NF.B.7c"];
    skillNumberMap['507'] = ["5.OA.A.2"];
    skillNumberMap['508'] = ["6.G.A.4"];
    skillNumberMap['509'] = ["7.RP.A.3", "7.EE.B.3"];
    skillNumberMap['510'] = ["7.RP.A.2c", "7.RP.A.2b"];
    skillNumberMap['511'] = ["7.G.A.1"];
    skillNumberMap['512'] = ["8.G.A.5"];
    skillNumberMap['513'] = ["8.EE.B.5"];
    skillNumberMap['514'] = ["8.EE.B.6"];
    skillNumberMap['515'] = ["4.NF.B.3c", "4.NF.B.3d"];
    skillNumberMap['516'] = ["5.OA.B.3"];
    skillNumberMap['517'] = ["5.G.A.1"];
    skillNumberMap['518'] = ["5.NF.B.3"];
    skillNumberMap['519'] = ["5.MD.C.5c", "5.MD.C.4", "5.MD.C.5b"];
    skillNumberMap['520'] = ["5.MD.A.1", "5.NBT.B.7"];
    skillNumberMap['521'] = ["5.NF.A.2", "5.NF.B.6"];
    skillNumberMap['522'] = ["7.EE.B.3", "7.NS.A.3"];
    skillNumberMap['523'] = ["6.RP.A.3b"];
    skillNumberMap['524'] = ["6.EE.B.5", "6.EE.B.6", "6.EE.B.7"];
    skillNumberMap['525'] = [];
    skillNumberMap['526'] = ["7.EE.B.4a", "7.EE.B.4b"];
    skillNumberMap['527'] = ["5.NF.B.4b", "5.NF.B.5a", "5.NF.B.5b", "5.NF.B.6"];
    skillNumberMap['528'] = ["4.NBT.B.4"];
    skillNumberMap['529'] = ["4.OA.A.3"];
    skillNumberMap['530'] = ["3.OA.A.2"];
    skillNumberMap['531'] = ["5.OA.A.1", "5.NBT.B.7", "5.NF.A.1", "5.NF.B.4a", "5.NF.B.7a", "5.NF.B.7b"];
    skillNumberMap['532'] = ["6.NS.B.2"];
    skillNumberMap['533'] = ["5.NBT.B.7"];
    skillNumberMap['534'] = ["5.NBT.B.7"];
    skillNumberMap['535'] = ["5.NBT.B.7"];
    skillNumberMap['536'] = ["6.SP.B.5c"];
    skillNumberMap['537'] = ["6.SP.A.2", "6.SP.B.5d", "6.SP.B.5a", "6.SP.B.5b", "6.SP.B.5c"];
    skillNumberMap['538'] = ["8.SP.A.1"];
    skillNumberMap['541'] = ["7.EE.A.1"];
    skillNumberMap['542'] = ["8.EE.A.3"];
    skillNumberMap['543'] = ["6.G.A.1", "6.G.A.3"];
    skillNumberMap['544'] = ["8.EE.C.8b"];
    skillNumberMap['545'] = ["7.RP.A.3"];
    skillNumberMap['546'] = ["7.EE.A.1"];
    skillNumberMap['547'] = ["HSA-REI.D.10", "HSF-IF.A.1", "HSF-IF.A.2", "HSF-IF.C.7a"];
    skillNumberMap['548'] = ["HSF-IF.A.1", "HSF-IF.A.2"];
    skillNumberMap['549'] = ["HSA-REI.B.4a", "HSA-REI.B.4b"];
    skillNumberMap['550'] = ["HSA-SSE.B.3a"];
    skillNumberMap['551'] = ["HSA-REI.B.4a", "HSA-REI.B.4b"];
    skillNumberMap['552'] = ["8.EE.A.1"];
    skillNumberMap['553'] = ["HSA-REI.C.5", "HSA-REI.C.6"];
    skillNumberMap['554'] = ["HSF-IF.B.6"];
    skillNumberMap['555'] = ["HSF-IF.C.9"];
    skillNumberMap['556'] = ["HSA-SSE.B.3b"];
    skillNumberMap['557'] = ["HSN-RN.B.3"];
    skillNumberMap['558'] = ["HSS-ID.C.7", "HSS-ID.C.8"];
    skillNumberMap['559'] = ["HSF-BF.B.4a"];
    skillNumberMap['560'] = ["HSF-BF.A.2", "HSF-IF.A.3"];
    skillNumberMap['561'] = ["HSF-IF.C.7b"];
    skillNumberMap['562'] = ["HSS-ID.B.5"];

    var noMapping = ["118","127","135","136","148","155","159","163","168","180","206","207","234","241","244","251","255","260","266","289","291","309","314","320","327","339","340","341","347","348","351","354","355","356","360","361","369","374","375","378","379","380"];

    function getStandardsForSkillNumber(skillNumber) {
        return skillNumberMap[skillNumber];
    }

    function contains(a, obj) {
        for (var i = 0; i < a.length; i++) {
            if (a[i] === obj) {
                return true;
            }
        }
        return false;
    }

    newClassroomsContent.forEach(function(content) {
        var skillNumber = content.taskInfo.extended.new_classrooms.skillNumber;
        var standards = getStandardsForSkillNumber(skillNumber);
        if (standards.length > 0) {
            content.standards = standards;
            db.content.save(content);
        } else if (contains(noMapping, skillNumber)) {
            noMatchCount++;
        }
    });

    print("Number of items with no match: " + noMatchCount);
}