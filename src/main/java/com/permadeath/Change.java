package com.permadeath;

/**
 * Los 50 cambios de dificultad. Ninguno quita habilidades al jugador (golpear, ver, moverse...):
 * todos hacen el mundo y los mobs más peligrosos.
 */
public enum Change {
    ZOMBIE_SPEED("Zombis veloces", "Los zombis se mueven un 35% más rápido."),
    SKELETON_HEALTH("Esqueletos resistentes", "Los esqueletos tienen 6 corazones extra de vida."),
    SPIDER_SPEED("Arañas ágiles", "Las arañas se mueven un 40% más rápido."),
    CREEPER_BLAST("Creepers explosivos", "Las explosiones de los creepers son mucho más grandes."),
    CREEPER_CHARGED("Creepers cargados", "El 35% de los creepers nuevos nacen cargados."),
    CREEPER_FUSE("Creepers de mecha corta", "Los creepers explotan en la mitad de tiempo."),
    CREEPER_SPEED("Creepers veloces", "Los creepers corren un 40% más rápido."),
    MOB_DAMAGE("Mobs más fuertes", "Los mobs hostiles hacen un 50% más de daño."),
    MOB_HEALTH("Mobs más robustos", "Los mobs hostiles tienen un 50% más de vida."),
    MOB_SPEED("Mobs más veloces", "Los mobs hostiles se mueven un 15% más rápido."),
    MOB_FOLLOW("Mirada lejana", "Los mobs hostiles te detectan desde mucho más lejos."),
    MOB_ARMOR("Piel endurecida", "Los mobs hostiles tienen armadura natural."),
    ZOMBIE_REINFORCE("Refuerzos zombi", "Los zombis llaman refuerzos casi siempre que los golpeas."),
    ZOMBIE_ARMOR("Zombis blindados", "Los zombis nuevos nacen con casco y peto de hierro."),
    SKELETON_HELMET("Esqueletos con casco", "Los esqueletos nuevos nacen con casco de hierro."),
    ZOMBIE_SWORD("Zombis armados", "Los zombis nuevos pueden nacer con espadas de hierro o diamante."),
    ZOMBIE_BABY("Zombis bebé", "El 35% de los zombis nuevos son bebés: pequeños y rapidísimos."),
    SUN_IMMUNE("Muertos inmunes al sol", "Zombis y esqueletos ya no se queman con la luz del día."),
    MOB_REGEN("Regeneración monstruosa", "Los mobs hostiles regeneran vida poco a poco."),
    MOB_PERSIST("Mobs persistentes", "Los mobs hostiles nuevos nunca desaparecen solos."),
    DAILY_SCALING("Escalado diario", "Los mobs hostiles ganan más vida con cada día que pasa."),
    PHANTOM_GIANT("Fantasmas gigantes", "Los fantasmas nacen mucho más grandes y hacen más daño."),
    NO_FALL("Mobs sin caída", "Los mobs hostiles no reciben daño por caída."),
    SPIDER_POISON("Arañas venenosas", "Los golpes de las arañas envenenan."),
    FIRE_ARROWS("Flechas incendiarias", "Las flechas de los esqueletos prenden fuego."),
    HEAVY_ARROWS("Flechas pesadas", "Las flechas de los esqueletos hacen más daño."),
    VAMPIRISM("Vampirismo", "Los mobs hostiles se curan al herirte."),
    BURNING_HITS("Golpes ardientes", "Los golpes de los mobs hostiles te incendian."),
    DEADLY_NIGHTS("Noches letales", "Durante la noche los mobs hostiles hacen un 50% más de daño."),
    EXPLOSIVE_DEATHS("Muertes explosivas", "Algunos mobs hostiles explotan al morir."),
    ZOMBIE_REVIVE("Zombis inmortales", "Algunos zombis se levantan otra vez al morir."),
    ZOMBIE_WAVES("Oleadas de zombis", "Aparecen zombis extra cerca de ti en la oscuridad."),
    SKELETON_AMBUSH("Emboscada de esqueletos", "Aparecen esqueletos extra cerca de ti en la oscuridad."),
    SPIDER_STALK("Arañas acechantes", "Aparecen arañas extra cerca de ti en la oscuridad."),
    CREEPER_STALK("Creepers acechantes", "Aparecen creepers extra cerca de ti en la oscuridad."),
    PHANTOM_SKY("Fantasmas del cielo", "Aparecen fantasmas sobre ti aunque duermas bien."),
    PILLAGER_ROAM("Saqueadores merodeadores", "Aparecen saqueadores con ballesta cerca de ti."),
    WITCH_ROAM("Brujas errantes", "Aparecen brujas cerca de ti."),
    ENDERMAN_ROAM("Endermans errantes", "Aparecen endermans en el mundo normal."),
    WITHER_SKELETONS("Esqueletos wither", "Aparecen esqueletos wither fuera del Nether."),
    VINDICATORS("Vindicadores", "Aparecen vindicadores con hacha cerca de ti."),
    RAVAGERS("Devastadores", "Aparecen devastadores cerca de ti."),
    SILVERFISH("Plaga de lepismas", "Aparecen lepismas extra cerca de ti en la oscuridad."),
    NIGHT_HORDE("Horda nocturna", "Al caer la noche aparece una gran horda a tu alrededor."),
    ETERNAL_STORM("Tormenta eterna", "El clima siempre es una tormenta eléctrica."),
    LIGHTNING("Cielo eléctrico", "Caen rayos cerca de ti de vez en cuando."),
    TNT_RAIN("Lluvia de TNT", "Cae TNT del cielo cerca de ti."),
    WILDFIRES("Incendios espontáneos", "Aparecen fuegos cerca de ti sin motivo."),
    SKY_GHASTS("Ghasts del cielo", "Aparecen ghasts volando sobre el mundo normal."),
    EVOKERS("Evocadores", "Aparecen evocadores cerca de ti.");

    public static final Change[] VALUES = values();

    public final String title;
    public final String description;

    Change(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
