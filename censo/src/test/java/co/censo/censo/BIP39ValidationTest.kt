package co.censo.censo

import androidx.test.core.app.ApplicationProvider
import co.censo.shared.util.BIP39InvalidReason
import co.censo.shared.util.BIP39
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BIP39ValidationTest {

    @Before
    fun setup() {
        BIP39.setup(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun `test bip39 too short`() {
        assertEquals(BIP39InvalidReason.TooShort(1), BIP39.validateSeedPhrase("wrong"))
    }

    @Test
    fun `test bip39 too long`() {
        assertEquals(BIP39InvalidReason.TooLong(25), BIP39.validateSeedPhrase("wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong"))
    }

    @Test
    fun `test bip39 bad length`() {
        assertEquals(BIP39InvalidReason.BadLength(13), BIP39.validateSeedPhrase("wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong"))
    }

    @Test
    fun `test bip39 invalid words`() {
        assertEquals(BIP39InvalidReason.InvalidWords(wordsByIndex = mapOf(10 to "wronger", 11 to "wrongest")), BIP39.validateSeedPhrase("wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wronger wrongest"))
    }

    @Test
    fun `test bip39 invalid checksum`() {
        assertEquals(BIP39InvalidReason.InvalidChecksum, BIP39.validateSeedPhrase("wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong wrong"))
    }

    @Test
    fun `test bip39 trims whitespace`() {
        assertNull(BIP39.validateSeedPhrase("   lizard size puppy joke venue census need net produce age all proof opinion promote setup flight tortoise multiply blanket problem defy arrest switch also   "))
    }

    @Test
    fun `test bip39 handles multiple spaces`() {
        assertNull(BIP39.validateSeedPhrase("lizard   size  puppy  joke venue census need\nnet produce age all proof opinion promote setup flight tortoise multiply   blanket problem defy arrest switch also"))
    }

    @Test
    fun `test phrase with entropy value with zero first byte`() {
        assertNull(BIP39.validateSeedPhrase("abstract mom mother cool quantum giant humble salute deputy cradle ride cricket"))
    }

    @Test
    fun `test to binary entropy round trip`() {
        testPhrases.forEach { phrase ->
            assertEquals(
                phrase,
                BIP39.binaryDataToWords(BIP39.wordsToBinaryData(BIP39.splitToWords(phrase))).joinToString(" ")
            )
        }
    }

    @Test
    fun `test language determination`() {
        enumValues<BIP39.WordListLanguage>().forEach { language ->
            assertEquals(language, BIP39.determineLanguage(languageTestPhrases[language]!!), )
        }
    }

    @Test
    fun `test languages`() {
        enumValues<BIP39.WordListLanguage>().forEach { language ->
            println(language.displayName())
            val phrase = languageTestPhrases[language]!!
            val words = BIP39.splitToWords(phrase)
            assertNull(BIP39.validateSeedPhrase(phrase))
            assertNull(BIP39.validateSeedPhrase(words))

            val binaryData = BIP39.wordsToBinaryData(words)
            assertEquals(binaryData[0], language.value)
            assertEquals(words, BIP39.binaryDataToWords(binaryData))

            enumValues<BIP39.WordListLanguage>().forEach { otherLanguage ->
                println("\t${language.displayName()}")
                assertEquals(
                    BIP39.binaryDataToWords(binaryData, otherLanguage),
                    BIP39.splitToWords(languageTestPhrases[otherLanguage]!!)
                )
            }
        }
    }

    @Test
    fun `test valid`() {
        testPhrases.forEach { phrase ->
            assertNull(BIP39.validateSeedPhrase(phrase))
        }
    }

    @Test
    fun `test generation`() {
        val phrase1 = BIP39.generate(BIP39.WordCount.Twelve, BIP39.WordListLanguage.English)
        assertEquals(12, phrase1.size)
        assertNull(BIP39.validateSeedPhrase(phrase1))
        assertNotEquals(phrase1, BIP39.generate(BIP39.WordCount.Twelve, BIP39.WordListLanguage.English))

        val phrase2 = BIP39.generate(BIP39.WordCount.TwentyFour, BIP39.WordListLanguage.English)
        assertEquals(24, phrase2.size)
        assertNull(BIP39.validateSeedPhrase(phrase2))
    }

    private val languageTestPhrases: Map<BIP39.WordListLanguage, String> = mapOf(
        BIP39.WordListLanguage.English to  "donor tower topic path obey intact lyrics list hair slice cluster grunt glare trap appear immense vibrant vendor document cushion arrow same link tissue",
        BIP39.WordListLanguage.Spanish to  "derecho tetera teoría odisea nasal jarra macho llorar guerra roble caspa grito género tigre altura imitar usar unidad delfín collar anciano pupila llegar tazón",
        BIP39.WordListLanguage.French to "déglutir syntaxe surmener muséum maximal grenat isoler innocent flairer remarque cendrier filmer farceur tambour alourdir géranium union tuyau déductif compact analyse potager inhiber strict",
        BIP39.WordListLanguage.Italian to "duna tifare teorema pilifero palazzina luminoso multiplo mondina india serraglio ciottolo incanto guaio titolo ammenda letterale vagabondo usanza dovuto custode anello salasso molosso tariffa",
        BIP39.WordListLanguage.Portugese to "cozinha tagarela surpresa moeda manequim goiaba intriga infinito ficheiro rebelde cadastro feriado externo tapar alameda gasoduto tutelar trunfo corvo chefe alienar poeira inerente subtrair",
        BIP39.WordListLanguage.Czech to "jepice vlnovka vjezd popel placenta nastat odsun odcizit metoda subtropy fond mazurka lucifer vojna bodlina nadobro zakoupit zadusit jednatel honitba brambora skluz odboj veselka",
        BIP39.WordListLanguage.Japanese to "けおとす　みもと　みつける　てはい　ちんもく　せっきゃく　だいたい　そんかい　しゃうん　ひそむ　かほう　しまう　しごと　むえん　いそがしい　すべて　ようちえん　ゆでる　けいこ　きまる　いっち　のりゆき　そよかぜ　まんが",
        BIP39.WordListLanguage.Korean to "문득 판단 특수 유난히 옥수수 스물 알루미늄 아스팔트 선풍기 지점 단어 선거 상업 팬티 결혼 수면 해당 함부로 무엇 마음 계곡 정장 아드님 통과",
        BIP39.WordListLanguage.ChineseSimplified to "望 枯 络 障 筒 屋 毒 伸 乎 彼 元 攻 医 欣 物 振 骑 诺 罪 称 把 辆 纳 钉",
        BIP39.WordListLanguage.ChineseTraditional to "望 枯 絡 障 筒 屋 毒 伸 乎 彼 元 攻 醫 欣 物 振 騎 諾 罪 稱 把 輛 納 釘"
    )

    private val testPhrases = listOf(
        "abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon abandon about",
        "zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo zoo wrong",
        "media squirrel pass doll leg across modify candy dash glass amused scorpion",
        "fantasy rain also faith churn acquire wolf salad switch skirt donate shield energy cart possible",
        "ugly pattern possible away witness sword manual soap spin dolphin thrive dinosaur blast tide century program note history",
        "mystery patch robot birth pair regret divide trap asthma know increase shoulder answer coin hire monster ask vintage weather stock arrow",
        "alarm habit butter brass nerve embrace purpose deliver sentence wide come filter crush comfort urban vendor supreme age ketchup wedding truly comfort mother zone",
        "whip spatial call cream base decorate tobacco life below lobster arena movie cat fix buffalo vibrant victory jungle category picnic way raise hazard exact",
        "aim rude alert bracket jaguar essay clinic swallow because goddess put tonight tuna cactus rapid primary bean neither roast clog coin spawn salmon network",
        "nut remain invest display issue offer cloth lava alpha rhythm minimum day raccoon alpha suit setup lunar remember cable describe pear song rocket ice",
        "educate melody wool cabin still heavy alert swamp inform cool adult dove adjust region olympic renew novel dwarf island slush glide vault renew ride",
        "waste slogan gallery habit album depend benefit pear liquid achieve remember reopen foster student pencil outer canal forget road meat hurt globe slam story",
        "exotic join safe search exile fatigue web bubble wool romance wheat once sea illegal cave super hedgehog together key guitar uniform claw conduct okay",
        "ribbon reunion add country betray pilot axis ice burden cricket use visit only caution length effort rule remove promote puppy father mouse skirt coyote",
        "pupil possible upon cancel win spirit bless noble fetch wool exercise diary tortoise salmon bracket sight wreck orbit december general quality ketchup discover track",
        "sound catalog rapid cake ugly celery warfare genuine record pretty lawn trumpet pepper twice muffin require afraid asthma camp innocent world fetch slice gown",
        "march hood cost fan into anchor dawn bomb debate toy kite lecture pattern west people bamboo small summer prefer traffic pig party seminar fancy",
        "process dynamic curtain pill define emerge raw column link robust execute caution owner civil reopen artwork suspect burden series business relax life motor chicken",
        "learn poet deputy page genius fantasy into twin student rule surprise fetch merge light giggle exile three coyote shove grid across dash subject donkey",
        "winner what artwork wise joy inform faint bottom void flight balance sunset soda shoot victory dove lonely tooth burger enroll autumn easily sweet table",
        "enrich conduct cycle warrior company tunnel word cement mom song tube sugar favorite swing web verb carry half various second mind term burden dolphin",
        "globe traffic number floor aisle entry identify kiss hour liar version license inhale opera toward wire appear faint diary asthma obscure admit kiss faculty",
        "disease hundred skate special blanket vehicle custom brass resist wire climb lawsuit shop slender friend poverty napkin pudding episode road evidence lawsuit egg waste",
        "vicious cargo kid female alarm grief dial split merit share grant measure what shed grace sting actual vanish dignity sample embark artist loud pig",
        "frown oxygen jar crop skin stool service anxiety camera trash calm hamster topic surge must theory female seed gather script expect smile august cloth",
        "cheap involve month script giant ramp sudden idea scare injury power rescue need evoke task often abstract debate approve lift hope height dignity scale",
        "true discover flee tube festival tank harsh canoe census mushroom bike boy bottom early auction refuse athlete catch ensure scrap bomb wash canal print",
        "equal uphold struggle taste chaos scout marble eye surface picnic swift indoor match social resource member wonder hood fiction couple better soldier divorce rib",
        "stone stamp drum segment control decrease way giggle shy inch cost kitten predict tube series woman veteran lemon sad disease mix train fox twin",
        "evil among neither gadget enroll museum angle toss bar broccoli fall easily obscure album symptom typical pattern gate park humor steak jealous belt twist",
        "heart direct eternal silk goddess onion measure dust oppose subway apart shell couch fly acquire rabbit rotate series neither cluster wink choose refuse oppose",
        "panel silly habit indicate beef illegal muscle comfort pig pear shaft wealth common remain consider myth nation stage across regular detail sister better day",
        "relax swim quit law chat blossom weather album satoshi endless salad blanket drill ankle asset vacant refuse other clog van miss inquiry stairs receive",
        "subway little street gather way net east horror rebel custom gauge moral candy virus toast gown step sadness slow curtain during page release egg",
        "hawk lottery solution oblige local sound forward peanut outdoor million market best torch breeze orbit mistake denial nephew measure crime cash mixture credit pony",
        "catch bacon organ art slab apple worry early fault oyster congress radar victory excess sweet scan soft rather human kidney quiz lion throw mushroom",
        "clip rookie unhappy jacket under minor blush bright nose resemble modify proof begin hidden predict vacant hello champion roof canoe supply submit glad wagon",
        "token yard easy forest mystery scene twenty host damp wise alley diary help melody debate shrug fringe ill prefer kit room bacon useless zebra",
        "ensure marble icon allow record chuckle uphold shoulder sample interest surprise jewel glide host wise lemon proud series ticket foil dilemma play system loan",
        "basic fantasy hamster guide aware meat helmet success rather trap labor diamond question ribbon hand spawn arena voice chalk master shock pact stereo senior",
        "wing old that trap half response typical able pretty girl mad face trumpet stock day question beach flame payment educate twelve lucky make filter",
        "include fork unhappy sick clutch vote canoe salmon enroll clock staff wonder magic toward grace ice noble auto siren scorpion gloom pride distance boring",
        "slide embark female food arrange manual holiday just person group circle grant sugar inmate soda style shuffle grit marble pass okay nuclear resist move",
        "dumb brave patch heart fee exclude repair ancient choice dial cost protect fault couch broccoli amount company october oxygen solid prepare artwork quantum father",
        "exact cushion hour electric suffer resemble grief middle imitate drill uncle trouble tiger flame dutch position embrace heart inflict chef few layer labor hill",
        "ankle bitter friend donate poverty coast sample expire surge squirrel fine release dawn evidence prize fog rival vicious upper purchase main sister chapter movie",
        "guide sheriff risk cactus celery country exile sunny promote chalk harbor amateur forward left can sign floor border early expect ritual episode tomato spider",
        "bus flavor atom sad armed drop jar stove spread breeze cage elegant rug fee organ cross label degree impulse echo cross candy window unfold",
        "option sunset token pair spider exclude yard dragon then build around shine diesel loyal target member gym turn someone ask draw avocado giggle market",
        "oppose unfold nerve spray logic suffer fame siren finger ski whip banner hip board spend ethics series veteran width pen museum ready month scatter",
        "suffer invest point unknown effort execute behind donor purpose tomato drive sustain brave rib use pupil door depend anxiety mixture custom upon feature cool",
        "mind token inquiry enlist carpet vocal future that chimney panel ostrich village shiver whip silent uncle section oxygen spread pepper assist depth always enact",
        "afford execute usual breeze blanket toilet area find apart immense loop gloom laundry skate expire better duck naive squirrel keep shuffle basket seek truly",
        "celery multiply same knife record device endless vocal move brand pioneer stuff sheriff rapid crouch ability kitchen between capital crush lift general face exile",
        "chaos elegant dinner wheat brief legal tribe suspect chase inmate balcony bulb glue spray manage jungle keep gym simple tank more soon road uncover",
        "bomb market bird reflect glance coast access fit hospital pottery crouch club salon page refuse impulse wood expose cereal slow dry pair blanket panel",
        "cute home person wonder stove lava clutch few release pumpkin fringe they cage follow echo leg episode asthma swamp skull illegal fold hobby unveil",
        "ladder excite sense have birth image response page myself oval thank length over zone toilet buddy interest interest number lawsuit ranch decade taste claim",
        "echo flat forget radio apology old until elite keep fine clock parent cereal ticket dutch whisper flock junior pet six uphold gorilla trend spare",
        "refuse hedgehog nerve insect silent sunset regret slush walnut illness visit slim advance mobile shrug initial grid topple inch okay bunker marriage bench chapter",
        "index magic script ghost burst turkey error fiction nose romance pelican smile egg glow body fatal sound tail point hedgehog original laugh abuse mass",
        "type river escape often wood pupil model elder true bamboo garage sense cage industry smart parade rotate blossom foam provide wine cushion office video",
        "novel volume cave position strike report shift denial banana bone use group clip lock envelope become room slide simple junior grab risk fresh eagle",
        "reject crush can matrix slam funny supreme mention network bitter aware orient dove mixed sick vocal balcony wonder castle plate era magnet road marriage",
        "security cart there boil afford purpose south trumpet tenant lounge wreck logic torch indoor tilt fix ostrich energy hole inch matter zero soccer canoe",
        "find mimic evoke knife bracket soon flag hunt crew valid subway like inhale step swarm noble slice trumpet dog magnet law scene royal copy",
        "foot salute anchor gorilla pistol snap answer feed evoke rigid wink garlic awful garage sand swarm quantum surprise among tumble evidence share brisk index",
        "must human alter prison fat drive divorce tourist aware husband fee flavor model notice truck wire barely dumb unit faint type sentence fragile slow",
        "bright enforce replace picnic school polar spatial improve grant bacon dilemma wolf erase clock police total deal nature finish survey assume please thunder prize",
        "mad print witness manage crash prevent kidney anchor banana vicious boat away produce girl engage skull aware crouch abandon easily various myth miss suit",
        "update gospel mandate wrong school robust paper near divert six salt picnic skirt action squirrel extend wink rifle ride giggle another antenna narrow soda",
        "detail pottery yard banner enhance remove guard until velvet unfair next metal old world travel desk duty volcano food kit replace vote amazing corn",
        "original expect cube brother monster aisle sample post shoulder chronic ripple opera author spice wrap winter dry give broccoli observe car small praise cotton",
        "hill aware matter travel oval brief leopard laundry off immense gadget observe ginger nothing inhale begin stick leave unfold say door novel exit absurd",
        "habit taste any west evidence legal rival unknown purpose dose write robot member vanish powder forum cigar elbow science benefit drama key dismiss gas",
        "dumb mask shaft vapor senior shove release mention logic pelican muffin royal avocado mouse educate maximum arena vacant alter since toast security upon stem",
        "enjoy dream kitten vocal music fantasy energy sail end timber vanish age cigar educate someone grass always runway wrist infant explain blossom scheme wheel",
        "lamp cup reason actress nasty spring bleak lecture since tribe need bench notable inject engage sunny order insect hamster nose promote catch balance planet",
        "lobster festival dice awful giant october razor enrich track scheme apart silent loan happy interest valve music fetch produce lens zoo slide demise blur",
        "wear fatal index crazy minimum merry fork gallery cross then parrot stem exact uphold case analyst walk admit pattern exit aspect winner scheme lamp",
        "replace various meat lion long monster afraid engage busy mouse shrimp wife burden notice album unknown hire slot weapon demise main fade reject sand",
        "cattle domain slice frost path radio pear payment phrase spy forward misery define permit project subject fancy exchange human clean rich hurdle rubber tilt",
        "scissors major identify north avocado flower negative distance best begin imitate uphold raw birth december roast tool local head forward salt motion rookie replace",
        "consider nut dry gap museum saddle valve oval scrub guard slam bonus slam luggage wing tank end roast desk divert title bid just clap",
        "zone hidden oil swift near crush tilt border public zone artist gather kite enable anger hospital dirt column lumber knife deposit rely find magnet",
        "chunk pulp rail afford primary dinosaur bean concert betray net burden salad couple wave rather giant frog supreme carry acoustic quick invite rapid planet",
        "mirror wing theme impact blind happy motion certain twist step lounge erupt athlete question month direct rabbit ivory garden critic reopen common dilemma author",
        "mixture amused purpose fan picnic visit seat grant humor inherit quit large sudden boost often pink couple sustain remove thumb habit course violin unlock",
        "maple panda wrong wish annual hip trim fire joke motor vehicle cost legend update lumber innocent slim shoot slow dwarf bid state unfold when",
        "stuff coconut accuse lawsuit craft rug forward tongue recycle clip flight tired flag vendor work banana detect sponsor winter viable barely brass abandon unfair",
        "search old slam life dwarf air torch tank decorate garbage diary bring ritual shed table seat sponsor steel favorite vintage learn remove control air",
        "subject affair spider memory second damage okay nasty creek because helmet point gate identify banana title refuse film become enact verify scissors matrix anger",
        "ask ritual ribbon enlist civil aware shop brown lyrics more seat index judge maid scatter heavy vapor twice embody loan disagree liberty move traffic",
        "fine question shop imitate coach husband food gauge original injury blossom burden million worth hip phone later valley axis jar build erase tool below",
        "auction knife manual foot suit return antenna mention spray segment panther palace voice napkin hover still vessel heart disorder lesson absurd audit cable patient",
        "call naive suit hello forward rug december web spell tail unfold dismiss art defense symptom safe large liquid army mention cup screen robust sphere",
        "unveil clump educate confirm gain garage comfort liquid position begin strike immune wink expand pond funny labor goddess switch soldier orange doll useless bid",
        "venture negative wedding venue stay vocal mutual crisp soccer cliff only enforce want gloom open hole odor water banner rubber rapid crack wood carry",
        "either trouble come animal garlic boil wonder drip wreck wrap slim april item orphan explain fatal debris absorb share actress bargain cement first weather",
        "saddle flavor material coral settle table wet aware pencil step boss dad earth demise seminar circle shield liar twist table depth news census have",
        "sample wreck parrot access glance sadness leaf rifle floor lunar moment opinion jazz above clinic slot segment few cabbage unaware result bacon topple hair",
        "legend fame cable lava mail energy scout scatter broccoli raven quality country crowd memory media core replace around gate car exclude congress marine myself",
        "tired response door defy crack romance project proof empty bicycle column found derive again need uniform pitch nature glide huge knife drama obey pepper",
        "lake sea approve topic music text permit camp mobile weapon pole decide section embrace ladder hub deputy loyal suit joy engine into physical much",
        "repair model congress stock seek inhale milk neutral finish test curtain matrix right chicken safe easily carpet pelican speed original exercise attitude witness similar",
        "dumb grow again raise obscure person gate couple maximum reason trophy off harsh raise replace pill rookie accident coast monitor pact sun average mesh",
        "lens ramp mind spread exile swear coral burger improve bottom mixed artist need fork message minute senior rack dish display auction wide suffer sword",
        "home ill chef steel note barely journey baby castle agree potato casino lab cover swallow mimic range van fiction bullet sleep age trophy catalog",
        "jelly luggage train cushion fetch this saddle patrol skirt shove organ visa misery subway despair student jacket public evidence square tribe setup coyote pool",
        "video sun cabbage celery debris double also merit order decorate slow uniform oval edit buffalo opera energy news salt canal dinner unaware filter spot",
        "okay screen blood six hybrid episode surface carbon silk valid alien street emotion echo magnet because velvet vast evoke health million vintage oxygen fit",
        "link rescue thunder service access kitchen original rotate tunnel have erode seed wage merge march nature hold build purity dial evidence topic planet between",
        )
}