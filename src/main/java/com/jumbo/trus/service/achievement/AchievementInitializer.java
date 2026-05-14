package com.jumbo.trus.service.achievement;

import com.jumbo.trus.entity.achievement.AchievementEntity;
import com.jumbo.trus.repository.achievement.AchievementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

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

                // nové achievementy
                new AchievementEntity("Klub sráčů", "KLUB_SRACU", true, "Celý tým po zápase vynechá třetí poločas", false),
                new AchievementEntity("Osamělý držák", "OSAMELY_DRZAK", true, "Jako jediný z týmu nevynechej třetí poločas", false),
                new AchievementEntity("Ve dvou se to lépe táhne", "VE_DVOU_SE_TO_LEPE_TAHNE", true, "Jako jediní dva nevynechejte třetí poločas a dejte si pivo", false),
                new AchievementEntity("Střelec", "STRELEC", true, "Měj nejvíce gólů za sezonu", false),
                new AchievementEntity("Fotr je lotr", "FOTR_JE_LOTR", true, "Získej kartu v zápase jakožto otec od rodiny", false),
                new AchievementEntity("Maratonec", "MARATONEC", true, "Uběhni v zápasech Trusu maraton", "Alespoň 42,1 km", false),
                new AchievementEntity("Roberto Carlos", "ROBERTO_CARLOS", true, "Zaznamenej v zápase střelu s rychlostí přes 80 km/h a gól", false),
                new AchievementEntity("Špílmachr", "SPILMACHR", true, "Zaznamenej v zápase alespoň 40 přihrávek",  false),
                new AchievementEntity("Já to za vás oběhal", "JA_TO_ZA_VAS_OBEHAL", true, "Měj v zápase nejvíce naběhaných kilometrů ze všech", "Alespoň 2 hráči s Footbarem", false),
                new AchievementEntity("Doplnění tekutin", "DOPLNENI_TEKUTIN", true, "Vypij po zápase alespoň tolik piv, kolik si naběhal kilometrů", "Uběhnuté alespoň 3 km", false),
                new AchievementEntity("Nástup jako hrom", "NASTUP_JAKO_HROM", true, "Ve svém prvním zápase za Trus dej gól", false),
                new AchievementEntity("Když leju tak pořádně", "KDYZ_LEJU_TAK_PORADNE", false, "Měj nejvyšší průměr vypitých piv/panáků na zápas v sezoně", false),
                new AchievementEntity("Machýrek", "MACHYREK", true, "Gól rabonou", false),
                new AchievementEntity("Sdílený střelec", "SDILENY_STRELEC", true, "Buď jedním z více hráčů, kteří vstřelí v zápase hattrick", false),
                new AchievementEntity("Nesobecký hrdina", "NESOBECKY_HRDINA", true, "Měj hattrick z asistencí", false),
                new AchievementEntity("Góly? Ne, raději pivo", "GOLY_NE_RADEJI_PIVO", true, "Měj nejvyšší průměr vypitých piv na gól v sezoně", "Alespoň 1 pivo a 1 gól", false),
                new AchievementEntity("Jarda Kužel", "JARDA_KUZEL", true, "Přijď na zápas po alespoň třech absencích a získej hvězdu utkání", false),
                new AchievementEntity("Moderní gólmanská škola", "MODERNI_GOLMANSKA_SKOLA", true, "Zaznamenej alespoň jednu asistenci jako brankář", false)
        );
    }
}