package com.jumbo.trus.service.achievement.init;

import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.jumbo.trus.service.achievement.AchievementCodes.*;

@Component
@RequiredArgsConstructor
public class AchievementInitializer implements CommandLineRunner {

    private final AchievementRepository achievementRepository;

    @Override
    public void run(String... args) {
        List<AchievementEntity> achievements = seedAchievements();

        List<AchievementEntity> newAchievements = achievements.stream()
                .filter(achievement -> !achievementRepository.existsByCode(achievement.getCode()))
                .toList();

        achievementRepository.saveAll(newAchievements);
    }

    private List<AchievementEntity> seedAchievements() {
        return List.of(
                new AchievementEntity("Každému, co mu patří", "KAZDEMU_CO_MU_PATRI", true, "Vypij přesně tolik piv/panáků, kolik si v zápase zaznamenal asistencí/gólů", "Musí být více než 1", false),
                new AchievementEntity("Fotbal je jen záminka", "FOTBAL_JE_JEN_ZAMINKA", false, "Nevynechej ani jednu návštěvu hospody v sezoně", "Alespoň v 8 zápasech", false),
                new AchievementEntity("Po pořádné práci pořádná oslava", "PO_PORADNE_PRACI_PORADNA_OSLAVA", false, "Vyhraj zápas s prvním mužstvem tabulky a vypij po zápase nejvíc piv/panáků", "Případně souboj mezi prvním a druhým", false),
                new AchievementEntity("Tahoun", "TAHOUN", false, "Vypij 3x po sobě nejvíce piv/panáků", false),
                new AchievementEntity("Kořala", "KORALA", false, "Vypij po zápase více panáků než piv", false),
                new AchievementEntity("Mecenáš", "MECENAS", true, "Zaplať nejvíce na pokutách za uplynulou sezonu", false),
                new AchievementEntity("Oslavenec", "OSLAVENEC", false, "Vypij po zápase víc piv/panáků než zbytek týmu dohromady", false),
                new AchievementEntity("Úspěšný den", "USPESNY_DEN", true, "Zaznamenej v zápase gól(čisté konto), žlutou kartu a v hospodě panáka a pivo", "Od každého aspoň jedno", false),
                new AchievementEntity("Černá práce", "CERNA_PRACE", true, "Dostaň hvězdu utkání i přes nulový počet vstřelených gólů", false),
                new AchievementEntity("Doping", "DOPING", true, "Dej hattrick / vychytej nulu s pokutou za kocovinu", false),
                new AchievementEntity("Autíčko", "AUTICKO", true, "Zaznamenej v zápase nejvíce kanadských bodů jako brankář", false),
                new AchievementEntity("Ožeň se, ožer se", "OZEN_SE_OZER_SE", true, "Dej si aspoň 8 kousků po svatbě", false),
                new AchievementEntity("Ross Geller", "ROSS_GELLER", true, "Nasbírej tři pokuty za svatby", false),
                new AchievementEntity("Čestný jako Karel Erben", "CESTNY_JAKO_KAREL_ERBEN", true, "Buď zmíněn v tisku za fair-play", true),
                new AchievementEntity("Zastřelování", "ZASTRELOVANI", true, "Měj v zápase min. 2 překopy a 2 góly", false),
                new AchievementEntity("Sobec", "SOBEC", true, "Měj v sezóně min 5 gólů a žádnou asistenci", false),
                new AchievementEntity("Nesobec", "NESOBEC", true, "Nasbírej v sezóně minimálně dvakrát tolik asistencí co gólů", "Alespoň 4 asistence", false),
                new AchievementEntity("Jen na skok", "JEN_NA_SKOK", true, "Příchod po výkopu + červená karta", false),
                new AchievementEntity("Hvězdné manýry", "HVEZDNE_MANYRY", true, "Přijď až po výkopu, ale i tak získej hvězdu zápasu", false),
                new AchievementEntity("Mirek Dušín", "MIREK_DUSIN", true, "Nejméně pokut v sezóně", false),
                new AchievementEntity("Konzistence", "KONZISTENCE", true, "Gól ve třech zápasech za sebou", false),
                new AchievementEntity("David Beckham", "DAVID_BECKHAM", true, "Zmínka v tisku + hvězda zápasu", false),
                new AchievementEntity("Dlouhá noc", "DLOUHA_NOC", true, "Kocovina + pozdní příchod", false),
                new AchievementEntity("Zbytečné prase", "ZBYTECNE_PRASE", true, "Červená karta, ale vyhraný zápas", false),
                new AchievementEntity("Den blbec", "DEN_BLBEC", true, "Alespoň 3 pokuty z kategorií pozdni příchod, karty, penalta, překop, nekompletní výbava, vlastňák", false),
                new AchievementEntity("Porouchaný budík", "POROUCHANY_BUDIK", true, "Tři pozdní příchody v sezoně", false),
                new AchievementEntity("Žlutý/Hnědý poplach", "ZLUTY_HNEDY_POPLACH", true, "Kocovina + vyprazdňování při zápase", false),
                new AchievementEntity("Sběratel", "SBERATEL", false, "Získej dva achievementy za zápas", false),
                new AchievementEntity("Medmrdka", "MEDMRDKA", true, "Dvě zmínky v tisku za sezonu", false),
                new AchievementEntity("Naroď se", "NAROD_SE", false, "Alias achievement útěchy - Pokuta za narozeniny", false),
                new AchievementEntity("Priority", "PRIORITY", false, "Účast na všech zápasech v sezóně", false),
                new AchievementEntity("Žlutá je dobrá", "ZLUTA_JE_DOBRA", true, "Zaznamenej v sezoně jak vyprazdňování při zápase tak žlutou kartu", false),
                new AchievementEntity("Ionťák", "IONTAK", true, "Zaznamenej v zápase alespoň jedno pivo, ale vynechej třetí poločas", false),
                new AchievementEntity("Sportovec", "SPORTOVEC", true, "Vstřel v sezoně víc gólů než vypiješ piv", false),
                new AchievementEntity("Proč?", "PROC", true, "Diskotéka, decibely a k tomu pivo, a pak maj takovýhle pudy. Přijď v podroušeném stavu na zápas a dostaň žlutou/červenou kartu", false),
                new AchievementEntity("Hladinka", "HLADINKA", true, "Přijď na zápas se zbytkáčem a dej si panáka na udržení hladinky", false),
                new AchievementEntity("Štěně", "STENE", true, "Vypij za sezonu aspoň jedno štěně (60 piv)", false),
                new AchievementEntity("Cirhóza", "CIRHOZA", false, "Jak jsem mohl vědět že si na ty roháče dá 5 rumů a selžou mu játra? Dej si aspoň 5 panáků a vynechej příští zápas", false),
                new AchievementEntity("Ten to perfektně kope. Říkal", "TEN_TO_PERFEKTNE_KOPE", true, "Neproměněná penalta v zápase", false),
                new AchievementEntity("Áda Větvička", "ADA_VETVICKA", false, "Osoulož spoluhráčovu družku", true),
                new AchievementEntity("Klub sráčů", "KLUB_SRACU", true, "Celý tým po zápase vynechá třetí poločas", false),
                new AchievementEntity("Osamělý držák", "OSAMELY_DRZAK", true, "Jako jediný z týmu nevynechej třetí poločas", false),
                new AchievementEntity("Ve dvou se to lépe táhne", "VE_DVOU_SE_TO_LEPE_TAHNE", true, "Jako jediní dva nevynechejte třetí poločas a dejte si pivo", false),
                new AchievementEntity("Střelec", "STRELEC", true, "Měj nejvíce gólů za sezonu", false),
                new AchievementEntity("Fotr je lotr", "FOTR_JE_LOTR", true, "Získej kartu v zápase jakožto otec od rodiny", false),
                new AchievementEntity("Maratonec", "MARATONEC", true, "Uběhni v zápasech Trusu maraton", "Alespoň 42,1 km", false),
                new AchievementEntity("Roberto Carlos", "ROBERTO_CARLOS", true, "Zaznamenej v zápase střelu s rychlostí přes 80 km/h a gól", false),
                new AchievementEntity("Špílmachr", "SPILMACHR", true, "Zaznamenej v zápase alespoň 40 přihrávek", false),
                new AchievementEntity("Já to za vás oběhal", "JA_TO_ZA_VAS_OBEHAL", true, "Měj v zápase nejvíce naběhaných kilometrů ze všech", "Alespoň 2 hráči s Footbarem", false),
                new AchievementEntity("Doplnění tekutin", "DOPLNENI_TEKUTIN", true, "Vypij po zápase alespoň tolik piv, kolik si naběhal kilometrů", "Uběhnuté alespoň 3 km", false),
                new AchievementEntity("Nástup jako hrom", "NASTUP_JAKO_HROM", true, "Ve svém prvním zápase za Trus dej gól", false),
                new AchievementEntity("Když leju tak pořádně", "KDYZ_LEJU_TAK_PORADNE", false, "Měj nejvyšší průměr vypitých piv/panáků na zápas v sezoně", false),
                new AchievementEntity("Machýrek", "MACHYREK", true, "Gól rabonou", false),
                new AchievementEntity("Sdílený střelec", "SDILENY_STRELEC", true, "Buď jedním z více hráčů, kteří vstřelí v zápase hattrick", false),
                new AchievementEntity("Nesobecký hrdina", "NESOBECKY_HRDINA", true, "Měj hattrick z asistencí", false),
                new AchievementEntity("Góly? Ne, raději pivo", "GOLY_NE_RADEJI_PIVO", true, "Měj nejvyšší průměr vypitých piv na gól v sezoně", "Alespoň 1 pivo a 1 gól", false),
                new AchievementEntity("Jarda Kužel", "JARDA_KUZEL", true, "Přijď na zápas po alespoň třech absencích a získej hvězdu utkání", false),
                new AchievementEntity("Moderní gólmanská škola", "MODERNI_GOLMANSKA_SKOLA", true, "Zaznamenej alespoň jednu asistenci jako brankář", false),
                new AchievementEntity("Morální podpora", "MORALNI_PODPORA", true, "I přes zranění/trest se přijď podívat na zápas", false),
                new AchievementEntity("Lazar na tribunách", "LAZAR_NA_TRIBUNACH", true, "Přijď se podívat alespoň na 3 zápasy v sezoně i přes zranění", false),
                new AchievementEntity("Jednou se začít musí", "JEDNOU_SE_ZACIT_MUSI", false, "Dej si alespoň jedno pivo", false),
                new AchievementEntity("Když ono to chutná", "KDYZ_ONO_TO_CHUTNA", false, "Vypij alespoň 50 piv", false),
                new AchievementEntity("Soudek", "SOUDEK", false, "Vypij alespoň 100 piv", false),
                new AchievementEntity("Cisterna", "CISTERNA", false, "Vypij alespoň 500 piv", false),
                new AchievementEntity("Přitvrdíme", "PRITVRDIME", false, "Jednou to přijít muselo. Přistálo to před tebe a nemoh si nic dělat. Dal sis prvního panáka", false),
                new AchievementEntity("Rumový nádeník", "RUMOVY_NADENIK", false, "Je třeba oslavit každé velké vítěztví. Tak sis dal aspoň 20 panáků", false),
                new AchievementEntity("Achievement Milana Čurdy", "ACHIEVEMENT_MILANA_CURDY", false, "Alespoň 50 panáků? Byl by na tebe hrdej!", false),
                new AchievementEntity("Hvězda co se nezdá", "HVEZDA_CO_SE_NEZDA", true, "Získej svoji první hvězdu utkání v životě", false),
                new AchievementEntity("Komplexní hráč", "KOMPLEXNI_HRAC", true, "Útok, záloha či obrana, tam všude seš jako doma. Měj v zápase jak gól, tak asistenci", false),
                new AchievementEntity("Ultrus", "ULTRUS", false, "Měj alespoň 30 účastí na zápase jako fanoušek", false),
                new AchievementEntity("Permice na Trus", "PERMICE_NA_TRUS", false, "Měj alespoň 10 účastí na zápase jako fanoušek", false),
                new AchievementEntity("Do počtu", "DO_POCTU", true, "V pěti utkáních za sebou nezískej ani jeden kanadský bod", false),
                new AchievementEntity("Hattrick Gordieho Howa", "HATTRICK_GORDIEHO_HOWA", true, "V jednom zápase gól, asistence a bitka (ve formě žlutý či červený)", false),
                new AchievementEntity("Americký fotbalista", "AMERICKY_FOTBALISTA", true, "Nasbírej alespoň 10 pokut za překop", false),
                new AchievementEntity("Alzheimer", "ALZHEIMER", true, "Dostaň pokutu za zapomenutí věcí nebo nekompletní výbavu", false),
                new AchievementEntity("Leo Beránek", "LEO_BERANEK", true, "Já mám nové boty, koupil jsem si nové boty. Konkrétně kopačky", false),
                new AchievementEntity("Černé geny", "CERNE_GENY", true, "Dosáhni v zápase maximální rychlosti sprintu alespoň 25 km/h", false),
                new AchievementEntity("Zahraniční pozorovatel", ZAHRANICNI_POZOROVATEL, false, "Připoj se k Trusí appce ze zahraničí", false),
                new AchievementEntity("Do Afriky na černošky", DO_AFRIKY_NA_CERNOSKY, false, "Připoj se k Trusí appce z Afriky", false),
                new AchievementEntity("Hedvábná stezka", HEDVABNA_STEZKA, false, "Následuj moderní trendy asijskou tour a připoj se k Trusí appce z Asie", false),
                new AchievementEntity("Američan z Vysočan", AMERICAN_Z_VYSOCAN, false, "Do Ameriky jezděj parníky...připoj se k Trusí appce z Ameriky", false),
                new AchievementEntity("Po stopách Diega", PO_STOPACH_DIEGA, false, "Byl to feťák nebo ne? Připoj se k Trusí appce z Jižní  Ameriky", false),
                new AchievementEntity("Trusí Amundsen", TRUSI_AMUNDSEN, false, "V Anktartidě je nádherně... akorát na to připojit se na Trusí appku", false),
                new AchievementEntity("Lišák a moře", LISAK_A_MORE, false, "Ano i Oceánie je kontinent a je vhodná k zapnutí Trusí appky", false)


                );
    }
}