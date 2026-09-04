package com.jumbo.trus.service.achievement.init;

import com.jumbo.trus.entity.achievement.AchievementCalculationScope;
import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.entity.outbox.OutboxAggregateType;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.jumbo.trus.service.achievement.AchievementCodes.*;

@Component
@RequiredArgsConstructor
public class AchievementInitializer implements CommandLineRunner {

    private final AchievementRepository achievementRepository;

    @Override
    public void run(String... args) {
        List<AchievementEntity> definitions = seedAchievements();

        Map<String, AchievementEntity> existingByCode =
                achievementRepository.findAll()
                        .stream()
                        .collect(Collectors.toMap(
                                AchievementEntity::getCode,
                                Function.identity()
                        ));

        List<AchievementEntity> entitiesToSave = new ArrayList<>();

        for (AchievementEntity definition : definitions) {
            AchievementEntity existing = existingByCode.get(definition.getCode());

            if (existing == null) {
                // Achievement ještě v DB není.
                entitiesToSave.add(definition);
                continue;
            }

            // Aktualizace nových i původních údajů.
            existing.setName(definition.getName());
            existing.setOnlyForPlayers(definition.getOnlyForPlayers());
            existing.setDescription(definition.getDescription());
            existing.setSecondaryCondition(definition.getSecondaryCondition());
            existing.setManually(definition.getManually());
            existing.setCategory(definition.getCategory());

            existing.setAchievementTypes(definition.getAchievementTypes());
            existing.setCalculationScope(definition.getCalculationScope());

            entitiesToSave.add(existing);
        }

        achievementRepository.saveAll(entitiesToSave);
    }

    private List<AchievementEntity> seedAchievements() {
        List<AchievementEntity> achievements = List.of(
                new AchievementEntity("Každému, co mu patří", KAZDEMU_CO_MU_PATRI, true, "Vypij přesně tolik piv/panáků, kolik si v zápase zaznamenal asistencí/gólů",
                        "Musí být více než 1", false, EnumSet.of(OutboxAggregateType.BEER, OutboxAggregateType.GOAL, OutboxAggregateType.MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Fotbal je jen záminka", FOTBAL_JE_JEN_ZAMINKA, false, "Nevynechej ani jednu návštěvu hospody v sezoně", "Alespoň v 8 zápasech",
                        false, EnumSet.of(OutboxAggregateType.BEER, OutboxAggregateType.MATCH), AchievementCalculationScope.SEASON),
                new AchievementEntity("Po pořádné práci pořádná oslava", PO_PORADNE_PRACI_PORADNA_OSLAVA, false, "Vyhraj zápas s prvním mužstvem tabulky a vypij po zápase nejvíc piv/panáků",
                        "Případně souboj mezi prvním a druhým", false, EnumSet.of(OutboxAggregateType.BEER, OutboxAggregateType.MATCH, OutboxAggregateType.FOOTBALL_MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Tahoun", TAHOUN, false, "Vypij 3x po sobě nejvíce piv/panáků",
                        false, EnumSet.of(OutboxAggregateType.BEER), AchievementCalculationScope.MATCH),
                new AchievementEntity("Kořala", KORALA, false, "Vypij po zápase více panáků než piv",
                        false, EnumSet.of(OutboxAggregateType.BEER), AchievementCalculationScope.MATCH),
                new AchievementEntity("Mecenáš", MECENAS, true, "Zaplať nejvíce na pokutách za uplynulou sezonu",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.SEASON),
                new AchievementEntity("Oslavenec", OSLAVENEC, false, "Vypij po zápase víc piv/panáků než zbytek týmu dohromady",
                        false, EnumSet.of(OutboxAggregateType.BEER), AchievementCalculationScope.MATCH),
                new AchievementEntity("Úspěšný den", USPESNY_DEN, true, "Zaznamenej v zápase gól(čisté konto), žlutou kartu a v hospodě panáka a pivo",
                        "Od každého aspoň jedno", false, EnumSet.of(OutboxAggregateType.BEER, OutboxAggregateType.RECEIVED_FINE, OutboxAggregateType.GOAL, OutboxAggregateType.FOOTBALL_MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Černá práce", CERNA_PRACE, true, "Dostaň hvězdu utkání i přes nulový počet vstřelených gólů",
                        false, EnumSet.of(OutboxAggregateType.GOAL, OutboxAggregateType.FOOTBALL_MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Doping", DOPING, true, "Dej hattrick / vychytej nulu s pokutou za kocovinu",
                        false, EnumSet.of(OutboxAggregateType.GOAL, OutboxAggregateType.RECEIVED_FINE, OutboxAggregateType.FOOTBALL_MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Autíčko", AUTICKO, true, "Zaznamenej v zápase nejvíce kanadských bodů jako brankář",
                        false, EnumSet.of(OutboxAggregateType.GOAL, OutboxAggregateType.FOOTBALL_MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Ožeň se, ožer se", OZEN_SE_OZER_SE, true, "Dej si aspoň 8 kousků po svatbě",
                        false, EnumSet.of(OutboxAggregateType.BEER, OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Ross Geller", ROSS_GELLER, true, "Nasbírej tři pokuty za svatby",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Čestný jako Karel Erben", CESTNY_JAKO_KAREL_ERBEN, true, "Buď zmíněn v tisku za fair-play",
                        true, EnumSet.of(OutboxAggregateType.OTHER), AchievementCalculationScope.OTHER),
                new AchievementEntity("Zastřelování", ZASTRELOVANI, true, "Měj v zápase min. 2 překopy a 2 góly",
                        false, EnumSet.of(OutboxAggregateType.GOAL, OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Sobec", SOBEC, true, "Měj v sezóně min 5 gólů a žádnou asistenci",
                        false, EnumSet.of(OutboxAggregateType.GOAL), AchievementCalculationScope.SEASON),
                new AchievementEntity("Nesobec", NESOBEC, true, "Nasbírej v sezóně minimálně dvakrát tolik asistencí co gólů", "Alespoň 4 asistence",
                        false, EnumSet.of(OutboxAggregateType.GOAL), AchievementCalculationScope.SEASON),
                new AchievementEntity("Jen na skok", JEN_NA_SKOK, true, "Příchod po výkopu + červená karta",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Hvězdné manýry", HVEZDNE_MANYRY, true, "Přijď až po výkopu, ale i tak získej hvězdu zápasu",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE, OutboxAggregateType.FOOTBALL_MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Mirek Dušín", MIREK_DUSIN, true, "Nejméně pokut v sezóně",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.SEASON),
                new AchievementEntity("Konzistence", KONZISTENCE, true, "Gól ve třech zápasech za sebou",
                        false, EnumSet.of(OutboxAggregateType.GOAL), AchievementCalculationScope.MATCH),
                new AchievementEntity("David Beckham", DAVID_BECKHAM, true, "Zmínka v tisku + hvězda zápasu",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE, OutboxAggregateType.FOOTBALL_MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Dlouhá noc", DLOUHA_NOC, true, "Kocovina + pozdní příchod",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Zbytečné prase", ZBYTECNE_PRASE, true, "Červená karta, ale vyhraný zápas",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE, OutboxAggregateType.FOOTBALL_MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Den blbec", DEN_BLBEC, true, "Alespoň 3 pokuty z kategorií pozdni příchod, karty, penalta, překop, nekompletní výbava, vlastňák",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Porouchaný budík", POROUCHANY_BUDIK, true, "Tři pozdní příchody v sezoně",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.SEASON),
                new AchievementEntity("Žlutý/Hnědý poplach", ZLUTY_HNEDY_POPLACH, true, "Kocovina + vyprazdňování při zápase",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Sběratel", SBERATEL, false, "Získej dva achievementy za zápas",
                        false, EnumSet.of(OutboxAggregateType.ALL), AchievementCalculationScope.MATCH),
                new AchievementEntity("Medmrdka", MEDMRDKA, true, "Dvě zmínky v tisku za sezonu",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.SEASON),
                new AchievementEntity("Naroď se", NAROD_SE, false, "Alias achievement útěchy - Pokuta za narozeniny",
                        false, EnumSet.of(OutboxAggregateType.MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Priority", PRIORITY, false, "Účast na všech zápasech v sezóně",
                        false, EnumSet.of(OutboxAggregateType.MATCH), AchievementCalculationScope.SEASON),
                new AchievementEntity("Žlutá je dobrá", ZLUTA_JE_DOBRA, true, "Zaznamenej v sezoně jak vyprazdňování při zápase tak žlutou kartu",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.SEASON),
                new AchievementEntity("Ionťák", IONTAK, true, "Zaznamenej v zápase alespoň jedno pivo, ale vynechej třetí poločas",
                        false, EnumSet.of(OutboxAggregateType.BEER, OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Sportovec", SPORTOVEC, true, "Vstřel v sezoně víc gólů než vypiješ piv",
                        false, EnumSet.of(OutboxAggregateType.BEER, OutboxAggregateType.GOAL), AchievementCalculationScope.SEASON),
                new AchievementEntity("Proč?", PROC, true, "Diskotéka, decibely a k tomu pivo, a pak maj takovýhle pudy. Přijď v podroušeném stavu na zápas a dostaň žlutou/červenou kartu",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Hladinka", HLADINKA, true, "Přijď na zápas se zbytkáčem a dej si panáka na udržení hladinky",
                        false, EnumSet.of(OutboxAggregateType.BEER, OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Štěně", STENE, true, "Vypij za sezonu aspoň jedno štěně (60 piv)",
                        false, EnumSet.of(OutboxAggregateType.BEER), AchievementCalculationScope.SEASON),
                new AchievementEntity("Cirhóza", CIRHOZA, false, "Jak jsem mohl vědět že si na ty roháče dá 5 rumů a selžou mu játra? Dej si aspoň 5 panáků a vynechej příští zápas",
                        false, EnumSet.of(OutboxAggregateType.BEER, OutboxAggregateType.MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Ten to perfektně kope. Říkal", TEN_TO_PERFEKTNE_KOPE, true, "Neproměněná penalta v zápase",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Áda Větvička", ADA_VETVICKA, false, "Osoulož spoluhráčovu družku",
                        true, EnumSet.of(OutboxAggregateType.OTHER), AchievementCalculationScope.OTHER),
                new AchievementEntity("Klub sráčů", KLUB_SRACU, true, "Celý tým po zápase vynechá třetí poločas",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE, OutboxAggregateType.MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Osamělý držák", OSAMELY_DRZAK, true, "Jako jediný z týmu nevynechej třetí poločas",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE, OutboxAggregateType.MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Ve dvou se to lépe táhne", VE_DVOU_SE_TO_LEPE_TAHNE, true, "Jako jediní dva nevynechejte třetí poločas a dejte si pivo",
                        false, EnumSet.of(OutboxAggregateType.BEER, OutboxAggregateType.RECEIVED_FINE, OutboxAggregateType.MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Střelec", STRELEC, true, "Měj nejvíce gólů za sezonu",
                        false, EnumSet.of(OutboxAggregateType.GOAL), AchievementCalculationScope.SEASON),
                new AchievementEntity("Fotr je lotr", FOTR_JE_LOTR, true, "Získej kartu v zápase jakožto otec od rodiny",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.ALL),
                new AchievementEntity("Maratonec", MARATONEC, true, "Uběhni v zápasech Trusu maraton", "Alespoň 42,1 km",
                        false, EnumSet.of(OutboxAggregateType.FOOTBAR), AchievementCalculationScope.ALL),
                new AchievementEntity("Roberto Carlos", ROBERTO_CARLOS, true, "Zaznamenej v zápase střelu s rychlostí přes 80 km/h a gól",
                        false, EnumSet.of(OutboxAggregateType.FOOTBAR, OutboxAggregateType.GOAL), AchievementCalculationScope.MATCH),
                new AchievementEntity("Špílmachr", SPILMACHR, true, "Zaznamenej v zápase alespoň 40 přihrávek",
                        false, EnumSet.of(OutboxAggregateType.FOOTBAR), AchievementCalculationScope.MATCH),
                new AchievementEntity("Já to za vás oběhal", JA_TO_ZA_VAS_OBEHAL, true, "Měj v zápase nejvíce naběhaných kilometrů ze všech", "Alespoň 2 hráči s Footbarem",
                        false, EnumSet.of(OutboxAggregateType.FOOTBAR), AchievementCalculationScope.MATCH),
                new AchievementEntity("Doplnění tekutin", DOPLNENI_TEKUTIN, true, "Vypij po zápase alespoň tolik piv, kolik si naběhal kilometrů", "Uběhnuté alespoň 3 km",
                        false, EnumSet.of(OutboxAggregateType.BEER, OutboxAggregateType.FOOTBAR), AchievementCalculationScope.MATCH),
                new AchievementEntity("Nástup jako hrom", NASTUP_JAKO_HROM, true, "Ve svém prvním zápase za Trus dej gól",
                        false, EnumSet.of(OutboxAggregateType.MATCH, OutboxAggregateType.GOAL), AchievementCalculationScope.MATCH),
                new AchievementEntity("Když leju tak pořádně", KDYZ_LEJU_TAK_PORADNE, false, "Měj nejvyšší průměr vypitých piv/panáků na zápas v sezoně",
                        false, EnumSet.of(OutboxAggregateType.BEER, OutboxAggregateType.MATCH), AchievementCalculationScope.SEASON),
                new AchievementEntity("Machýrek", MACHYREK, true, "Gól rabonou",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Sdílený střelec", SDILENY_STRELEC, true, "Buď jedním z více hráčů, kteří vstřelí v zápase hattrick",
                        false, EnumSet.of(OutboxAggregateType.MATCH, OutboxAggregateType.GOAL), AchievementCalculationScope.MATCH),
                new AchievementEntity("Nesobecký hrdina", NESOBECKY_HRDINA, true, "Měj hattrick z asistencí",
                        false, EnumSet.of(OutboxAggregateType.GOAL), AchievementCalculationScope.MATCH),
                new AchievementEntity("Góly? Ne, raději pivo", GOLY_NE_RADEJI_PIVO, true, "Měj nejvyšší průměr vypitých piv na gól v sezoně", "Alespoň 1 pivo a 1 gól",
                        false, EnumSet.of(OutboxAggregateType.BEER, OutboxAggregateType.GOAL), AchievementCalculationScope.SEASON),
                new AchievementEntity("Jarda Kužel", JARDA_KUZEL, true, "Přijď na zápas po alespoň třech absencích a získej hvězdu utkání",
                        false, EnumSet.of(OutboxAggregateType.MATCH, OutboxAggregateType.FOOTBALL_MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Moderní gólmanská škola", MODERNI_GOLMANSKA_SKOLA, true, "Zaznamenej alespoň jednu asistenci jako brankář",
                        false, EnumSet.of(OutboxAggregateType.GOAL, OutboxAggregateType.FOOTBALL_MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Morální podpora", MORALNI_PODPORA, true, "I přes zranění/trest se přijď podívat na zápas",
                        false, EnumSet.of(OutboxAggregateType.MATCH, OutboxAggregateType.FOOTBALL_MATCH, OutboxAggregateType.PLAYER), AchievementCalculationScope.MATCH),
                new AchievementEntity("Lazar na tribunách", LAZAR_NA_TRIBUNACH, true, "Přijď se podívat alespoň na 3 zápasy v sezoně i přes zranění",
                        false, EnumSet.of(OutboxAggregateType.MATCH, OutboxAggregateType.FOOTBALL_MATCH, OutboxAggregateType.PLAYER), AchievementCalculationScope.SEASON),
                new AchievementEntity("Jednou se začít musí", JEDNOU_SE_ZACIT_MUSI, false, "Dej si alespoň jedno pivo",
                        false, EnumSet.of(OutboxAggregateType.BEER), AchievementCalculationScope.ALL),
                new AchievementEntity("Když ono to chutná", KDYZ_ONO_TO_CHUTNA, false, "Vypij alespoň 50 piv",
                        false, EnumSet.of(OutboxAggregateType.BEER), AchievementCalculationScope.ALL),
                new AchievementEntity("Soudek", SOUDEK, false, "Vypij alespoň 100 piv",
                        false, EnumSet.of(OutboxAggregateType.BEER), AchievementCalculationScope.ALL),
                new AchievementEntity("Cisterna", CISTERNA, false, "Vypij alespoň 500 piv",
                        false, EnumSet.of(OutboxAggregateType.BEER), AchievementCalculationScope.ALL),
                new AchievementEntity("Přitvrdíme", PRITVRDIME, false, "Jednou to přijít muselo. Přistálo to před tebe a nemoh si nic dělat. Dal sis prvního panáka",
                        false, EnumSet.of(OutboxAggregateType.BEER), AchievementCalculationScope.ALL),
                new AchievementEntity("Rumový nádeník", RUMOVY_NADENIK, false, "Je třeba oslavit každé velké vítěztví. Tak sis dal aspoň 20 panáků",
                        false, EnumSet.of(OutboxAggregateType.BEER), AchievementCalculationScope.ALL),
                new AchievementEntity("Achievement Milana Čurdy", ACHIEVEMENT_MILANA_CURDY, false, "Alespoň 50 panáků? Byl by na tebe hrdej!",
                        false, EnumSet.of(OutboxAggregateType.BEER), AchievementCalculationScope.ALL),
                new AchievementEntity("Hvězda co se nezdá", HVEZDA_CO_SE_NEZDA, true, "Získej svoji první hvězdu utkání v životě",
                        false, EnumSet.of(OutboxAggregateType.FOOTBALL_MATCH), AchievementCalculationScope.ALL),
                new AchievementEntity("Komplexní hráč", KOMPLEXNI_HRAC, true, "Útok, záloha či obrana, tam všude seš jako doma. Měj v zápase jak gól, tak asistenci",
                        false, EnumSet.of(OutboxAggregateType.GOAL), AchievementCalculationScope.MATCH),
                new AchievementEntity("Ultrus", ULTRUS, false, "Měj alespoň 30 účastí na zápase jako fanoušek",
                        false, EnumSet.of(OutboxAggregateType.MATCH, OutboxAggregateType.PLAYER), AchievementCalculationScope.ALL),
                new AchievementEntity("Permice na Trus", PERMICE_NA_TRUS, false, "Měj alespoň 10 účastí na zápase jako fanoušek",
                        false, EnumSet.of(OutboxAggregateType.MATCH, OutboxAggregateType.PLAYER), AchievementCalculationScope.ALL),
                new AchievementEntity("Do počtu", DO_POCTU, true, "V pěti utkáních za sebou nezískej ani jeden kanadský bod",
                        false, EnumSet.of(OutboxAggregateType.MATCH, OutboxAggregateType.GOAL), AchievementCalculationScope.MATCH),
                new AchievementEntity("Hattrick Gordieho Howa", HATTRICK_GORDIEHO_HOWA, true, "V jednom zápase gól, asistence a bitka (ve formě žlutý či červený)",
                        false, EnumSet.of(OutboxAggregateType.GOAL, OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Americký fotbalista", AMERICKY_FOTBALISTA, true, "Nasbírej alespoň 10 pokut za překop",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.ALL),
                new AchievementEntity("Alzheimer", ALZHEIMER, true, "Dostaň pokutu za zapomenutí věcí nebo nekompletní výbavu",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Leo Beránek", LEO_BERANEK, true, "Já mám nové boty, koupil jsem si nové boty. Konkrétně kopačky",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Černé geny", CERNE_GENY, true, "Dosáhni v zápase maximální rychlosti sprintu alespoň 25 km/h",
                        false, EnumSet.of(OutboxAggregateType.FOOTBAR), AchievementCalculationScope.MATCH),
                new AchievementEntity("Zahraniční pozorovatel", ZAHRANICNI_POZOROVATEL, false, "Připoj se k Trusí appce ze zahraničí",
                        false, EnumSet.of(OutboxAggregateType.OTHER), AchievementCalculationScope.OTHER),
                new AchievementEntity("Do Afriky na černošky", DO_AFRIKY_NA_CERNOSKY, false, "Připoj se k Trusí appce z Afriky",
                        false, EnumSet.of(OutboxAggregateType.OTHER), AchievementCalculationScope.OTHER),
                new AchievementEntity("Hedvábná stezka", HEDVABNA_STEZKA, false, "Následuj moderní trendy asijskou tour a připoj se k Trusí appce z Asie",
                        false, EnumSet.of(OutboxAggregateType.OTHER), AchievementCalculationScope.OTHER),
                new AchievementEntity("Američan z Vysočan", AMERICAN_Z_VYSOCAN, false, "Do Ameriky jezděj parníky...připoj se k Trusí appce z Ameriky",
                        false, EnumSet.of(OutboxAggregateType.OTHER), AchievementCalculationScope.OTHER),
                new AchievementEntity("Po stopách Diega", PO_STOPACH_DIEGA, false, "Byl to feťák nebo ne? Připoj se k Trusí appce z Jižní  Ameriky",
                        false, EnumSet.of(OutboxAggregateType.OTHER), AchievementCalculationScope.OTHER),
                new AchievementEntity("Trusí Amundsen", TRUSI_AMUNDSEN, false, "V Anktartidě je nádherně... akorát na to připojit se na Trusí appku",
                        false, EnumSet.of(OutboxAggregateType.OTHER), AchievementCalculationScope.OTHER),
                new AchievementEntity("Lišák a moře", LISAK_A_MORE, false, "Ano i Oceánie je kontinent a je vhodná k zapnutí Trusí appky",
                        false, EnumSet.of(OutboxAggregateType.OTHER), AchievementCalculationScope.OTHER),
                new AchievementEntity("Návštěva Sahary", NAVSTEVA_SAHARY, true, "Zúčastni se zápasu, při kterém teplota překročí 35 °C",
                        false, EnumSet.of(OutboxAggregateType.MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Ledový muž", LEDOVY_MUZ, true, "Zúčastni se zápasu, při kterém teplota klesla pod 0 °C",
                        false, EnumSet.of(OutboxAggregateType.MATCH), AchievementCalculationScope.MATCH),
                new AchievementEntity("Pošetření sil", POSETRENI_SIL, false, "Uběhni v zápase více kilometrů, než si ušel za poslední 2 dny", "Alespoň 2 km v zápase",
                        false, EnumSet.of(OutboxAggregateType.FOOTBAR, OutboxAggregateType.STEP), AchievementCalculationScope.MATCH),
                new AchievementEntity("Chodec", CHODEC, false, "Měj nachozeno nejvíce kroků ze všech mezi posledními zápasy", "Alespoň 3 chodci",
                        false, EnumSet.of(OutboxAggregateType.MATCH, OutboxAggregateType.STEP), AchievementCalculationScope.MATCH),
                new AchievementEntity("Okolo Hradce", OKOLO_HRADCE, false, "... v malé zahrádce... Ujdi vzdálenost jakoby to bylo okolo hradce (65 000 kroků)",
                        false, EnumSet.of(OutboxAggregateType.STEP), AchievementCalculationScope.ALL),
                new AchievementEntity("Pražák", PRAZAK, false, "Prahu musíš znáš skrz naskrz.Dokaž ji projít celou po jejím obvodu (160 000 kroků)",
                        false, EnumSet.of(OutboxAggregateType.STEP), AchievementCalculationScope.ALL),
                new AchievementEntity("Od severu k jihu", OD_SEVERU_K_JIHU, false, "Projdi se z malebného Šluknova až do Vyššího brodu na lodičky. To je úctyhodných 341 000 kroků",
                        false, EnumSet.of(OutboxAggregateType.STEP), AchievementCalculationScope.ALL),
                new AchievementEntity("Od východu na západ", OD_VYCHODU_NA_ZAPAD, false, "Kdyby ještě existovalo Československo, tak by to byl lepší výkon. Ale i tak musíš zvládnout ujít vzdálenost mezi Jablunkovem a Ašem - 612 000 kroků",
                        false, EnumSet.of(OutboxAggregateType.STEP), AchievementCalculationScope.ALL),
                new AchievementEntity("Všechny cesty vedou do Říma", VSECHNY_CESTY_VEDOU_DO_RIMA, false, "Ujdi cestu z Prahy až do Říma (1,6 milionu kroků)",
                        false, EnumSet.of(OutboxAggregateType.STEP), AchievementCalculationScope.ALL),
                new AchievementEntity("Evropský pochůzkář", EVROPSKY_POCHUZKAR, false, "Dokaž si spojit 2 nejvzálenější body Evropy. Ze Španělska až na Ural. 7,2 milionu kroků",
                        false, EnumSet.of(OutboxAggregateType.STEP), AchievementCalculationScope.ALL),
                new AchievementEntity("Cesta kolem světa", CESTA_KOLEM_SVETA, false, "Pro splnění stačí ujít 51,38 milionu kroků. Někteří to jiště zvládnou za rok 2x",
                        false, EnumSet.of(OutboxAggregateType.STEP), AchievementCalculationScope.ALL),
                new AchievementEntity("TrusBot", TRUSBOT, false, "Trus na Botě? Ne - pokecej si s AI!",
                        false, EnumSet.of(OutboxAggregateType.OTHER), AchievementCalculationScope.OTHER),
                new AchievementEntity("AI expert", AI_EXPERT, false, "Donuť TrusBota aby ti dal tento achievement.",
                        false, EnumSet.of(OutboxAggregateType.OTHER), AchievementCalculationScope.OTHER),
                new AchievementEntity("Týmový hráč", TYMOVY_HRAC, true, "Vynechej v sezoně maximálně 1 zápas a připiš si alespoň 5 asistencí",
                        false, EnumSet.of(OutboxAggregateType.MATCH, OutboxAggregateType.GOAL, OutboxAggregateType.PLAYER), AchievementCalculationScope.SEASON),
                new AchievementEntity("Flákač", FLAKAC, true, "Přijď po začátku a vynechej třetí poločas",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE), AchievementCalculationScope.MATCH),
                new AchievementEntity("Málo času, hodně muziky", MALO_CASU_HODNE_MUZIKY, true, "Přijď po začátku a získej alespoň 2 z těchto věcí: kartu, gól, asistenci",
                        false, EnumSet.of(OutboxAggregateType.RECEIVED_FINE, OutboxAggregateType.GOAL), AchievementCalculationScope.MATCH)

        );

        achievements.forEach(achievement ->
                achievement.setCategory(AchievementCategoryResolver.resolve(achievement))
        );

        return achievements;
    }
}
