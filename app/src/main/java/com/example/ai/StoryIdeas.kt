package com.example.ai

data class StoryIdeaItem(
    val id: String,
    val title: String,
    val category: String,
    val prompt: String,
    val recommendedAge: String,
    val recommendedTone: String,
    val defaultCharName: String,
    val defaultCharType: String,
    val defaultCharAppearance: String
)

data class StoryTemplate(
    val id: String,
    val title: String,
    val category: String,
    val iconName: String,
    val description: String,
    val defaultPrompt: String,
    val recommendedAge: String,
    val recommendedTone: String,
    val artStyle: String,
    val pageCount: Int,
    val colorHex: Long
)

object StoryDataBank {
    val STORY_IDEAS = listOf(
        StoryIdeaItem(
            id = "fox_stars",
            title = "The Fox Who Found the Stars",
            category = "Bedtime",
            prompt = "A little orange fox discovers a fallen baby star and helps it find its way back to the sky with the help of a wise old owl.",
            recommendedAge = "4–6 years",
            recommendedTone = "Gentle",
            defaultCharName = "Lumi",
            defaultCharType = "Animal",
            defaultCharAppearance = "Small orange fox with a soft sky-blue knitted scarf"
        ),
        StoryIdeaItem(
            id = "rabbit_dark",
            title = "Barnaby's Glow-in-the-Dark Ears",
            category = "Animals",
            prompt = "A timid little rabbit who is afraid of shadows discovers that when he hums a happy song, fireflies dance around him to light up the woods.",
            recommendedAge = "2–4 years",
            recommendedTone = "Gentle",
            defaultCharName = "Barnaby",
            defaultCharType = "Animal",
            defaultCharAppearance = "Fluffy white bunny with long floppy ears wearing yellow overalls"
        ),
        StoryIdeaItem(
            id = "cloud_baker",
            title = "The Cloud Baker's Giant Cupcake",
            category = "Fantasy",
            prompt = "An imaginative girl climbs a rainbow ladder to help a friendly baker who bakes marshmallow clouds that rain sweet strawberry milkdrops.",
            recommendedAge = "4–6 years",
            recommendedTone = "Magical",
            defaultCharName = "Mia",
            defaultCharType = "Human",
            defaultCharAppearance = "7-year-old girl with curly brown hair in two buns, wearing a polka-dot apron"
        ),
        StoryIdeaItem(
            id = "dino_tickles",
            title = "The T-Rex Who Loved Tickles",
            category = "Dinosaurs",
            prompt = "A big green dinosaur thinks he's too ferocious for games, until a tiny blue hummingbird tickles his chin and makes everyone laugh.",
            recommendedAge = "4–6 years",
            recommendedTone = "Funny",
            defaultCharName = "Rexie",
            defaultCharType = "Animal",
            defaultCharAppearance = "Chubby green baby T-Rex with friendly purple spots and tiny arms"
        ),
        StoryIdeaItem(
            id = "space_puppy",
            title = "Cosmo the Astronaut Puppy",
            category = "Space",
            prompt = "A golden retriever pup wearing a shiny bubble helmet floats past the rings of Saturn to rescue a lost space-satellite with a squeaky ball.",
            recommendedAge = "6–8 years",
            recommendedTone = "Adventurous",
            defaultCharName = "Cosmo",
            defaultCharType = "Animal",
            defaultCharAppearance = "Golden puppy wearing a white spacesuit with silver boots and clear helmet"
        ),
        StoryIdeaItem(
            id = "kindness_tree",
            title = "The Whispering Kindness Tree",
            category = "Learning",
            prompt = "Every time someone in Maple Glen shares a toy or says something kind, a shimmering golden leaf blooms on the central village tree.",
            recommendedAge = "6–8 years",
            recommendedTone = "Educational",
            defaultCharName = "Noah",
            defaultCharType = "Human",
            defaultCharAppearance = "8-year-old boy in a green cardigan with round glasses and a bright smile"
        ),
        StoryIdeaItem(
            id = "ocean_pearl",
            title = "Pip & the Singing Whale",
            category = "Ocean",
            prompt = "A tiny seahorse with rainbow fins helps a gentle blue whale remember the lost melody of the coral reef lullaby.",
            recommendedAge = "4–6 years",
            recommendedTone = "Magical",
            defaultCharName = "Pip",
            defaultCharType = "Fantasy Creature",
            defaultCharAppearance = "Tiny pastel coral seahorse with translucent glowing fins and a seashell crown"
        ),
        StoryIdeaItem(
            id = "robot_heart",
            title = "Beep-Boop's Colorful Garden",
            category = "Friendship",
            prompt = "A shiny little metal robot programmed only to count rocks learns how to water flowers and discovers feelings of joyful friendship.",
            recommendedAge = "6–8 years",
            recommendedTone = "Emotional",
            defaultCharName = "Beep-Boop",
            defaultCharType = "Robot",
            defaultCharAppearance = "Retro round silver robot with a cheerful digital smiley screen and antenna"
        )
    )

    val TEMPLATES = listOf(
        StoryTemplate(
            id = "magical_adventure",
            title = "Magical Adventure",
            category = "Adventure",
            iconName = "AutoAwesome",
            description = "Enchanted forests, glowing maps, and heroic young explorers discovering hidden wonders.",
            defaultPrompt = "A brave child finds an antique pocket compass that points toward secret doors hidden in hollow trees.",
            recommendedAge = "6–8 years",
            recommendedTone = "Adventurous",
            artStyle = "Fantasy",
            pageCount = 10,
            colorHex = 0xFF4F46E5
        ),
        StoryTemplate(
            id = "cute_animals",
            title = "Cute Animals",
            category = "Animals",
            iconName = "Pets",
            description = "Fluffy woodland creatures sharing wholesome tea parties, treehouse games, and cozy moments.",
            defaultPrompt = "Three animal neighbors prepare a surprise birthday berry pie for Mrs. Hedgehog.",
            recommendedAge = "2–4 years",
            recommendedTone = "Gentle",
            artStyle = "Watercolor",
            pageCount = 8,
            colorHex = 0xFF10B981
        ),
        StoryTemplate(
            id = "bedtime_dreams",
            title = "Bedtime Dreams",
            category = "Bedtime",
            iconName = "NightsStay",
            description = "Soft lulling rhythms, sleepy moonbeams, and snuggly blankets designed to help little ones drift off.",
            defaultPrompt = "The moon sends down a silver ladder so dreams can be delivered into every cozy bedroom.",
            recommendedAge = "2–4 years",
            recommendedTone = "Gentle",
            artStyle = "Watercolor",
            pageCount = 8,
            colorHex = 0xFF6366F1
        ),
        StoryTemplate(
            id = "space_odyssey",
            title = "Space Adventure",
            category = "Space",
            iconName = "RocketLaunch",
            description = "Rockets, swirling galaxies, friendly martians, and zero-gravity fun across the cosmos.",
            defaultPrompt = "Two curious siblings build a cardboard rocket that zooms off to inspect Saturn's glittering rings.",
            recommendedAge = "6–8 years",
            recommendedTone = "Exciting",
            artStyle = "3D Animated",
            pageCount = 12,
            colorHex = 0xFF0284C7
        ),
        StoryTemplate(
            id = "dino_world",
            title = "Dinosaur World",
            category = "Dinosaurs",
            iconName = "Park",
            description = "Prehistoric jungles, friendly herbivores, and playful dino hide-and-seek among the giant ferns.",
            defaultPrompt = "A little triceratops with striped horns helps his herd navigate through a foggy prehistoric valley.",
            recommendedAge = "4–6 years",
            recommendedTone = "Funny",
            artStyle = "Cartoon",
            pageCount = 10,
            colorHex = 0xFFF59E0B
        ),
        StoryTemplate(
            id = "princess_castle",
            title = "Princess & Castle",
            category = "Fairy Tale",
            iconName = "Castle",
            description = "Enchanted royal kingdoms, friendly dragons, and kind young royals who solve problems with empathy.",
            defaultPrompt = "Princess Nora invents a new telescope so the kingdom can watch shooting stars over the castle turrets.",
            recommendedAge = "4–6 years",
            recommendedTone = "Magical",
            artStyle = "Storybook",
            pageCount = 10,
            colorHex = 0xFFEC4899
        ),
        StoryTemplate(
            id = "ocean_adventure",
            title = "Ocean Adventure",
            category = "Ocean",
            iconName = "Water",
            description = "Coral kingdoms, sparkling jellyfish lanterns, sunken treasures, and friendly marine friends.",
            defaultPrompt = "A young otter searches for the legendary Pearl of Laughter in the bioluminescent coral caves.",
            recommendedAge = "4–6 years",
            recommendedTone = "Adventurous",
            artStyle = "Watercolor",
            pageCount = 10,
            colorHex = 0xFF06B6D4
        ),
        StoryTemplate(
            id = "jungle_safari",
            title = "Jungle Adventure",
            category = "Adventure",
            iconName = "Forest",
            description = "Canopy bridges, chattering monkeys, and colorful parrots guiding a trek through lush rain forests.",
            defaultPrompt = "A little leopard cub with mismatched spots discovers his special talent for climbing the tallest kapok tree.",
            recommendedAge = "6–8 years",
            recommendedTone = "Adventurous",
            artStyle = "Cartoon",
            pageCount = 10,
            colorHex = 0xFF059669
        ),
        StoryTemplate(
            id = "friendship_kindness",
            title = "Friendship & Kindness",
            category = "Friendship",
            iconName = "Favorite",
            description = "Learning to share, resolving disagreements, and realizing how much brighter life is with friends.",
            defaultPrompt = "Two playground rivals discover they both love painting dinosaurs and build a giant mural together.",
            recommendedAge = "4–6 years",
            recommendedTone = "Gentle",
            artStyle = "Storybook",
            pageCount = 8,
            colorHex = 0xFFF97316
        ),
        StoryTemplate(
            id = "school_adventure",
            title = "School Adventure",
            category = "Learning",
            iconName = "School",
            description = "First day of school jitters, exciting science fairs, recess games, and welcoming new classmates.",
            defaultPrompt = "A shy boy brings his pet snail for Show-and-Tell and ends up winning the hearts of the entire class.",
            recommendedAge = "6–8 years",
            recommendedTone = "Emotional",
            artStyle = "Colored Pencil",
            pageCount = 8,
            colorHex = 0xFF8B5CF6
        ),
        StoryTemplate(
            id = "superhero_kids",
            title = "Superhero Kids",
            category = "Exciting",
            iconName = "Bolt",
            description = "Capes, cool gadget belts, and everyday superpowers like courage, empathy, and honesty.",
            defaultPrompt = "Captain Clara and her trusty wonder-dog save the neighborhood treehouse from a gust of mischievous wind.",
            recommendedAge = "6–8 years",
            recommendedTone = "Exciting",
            artStyle = "Cartoon",
            pageCount = 12,
            colorHex = 0xFFEF4444
        ),
        StoryTemplate(
            id = "classic_fairy_tale",
            title = "Fairy Tale",
            category = "Fairy Tale",
            iconName = "AutoStories",
            description = "Timeless folklore magic with talking teacups, glass slippers, and golden keys to hidden realms.",
            defaultPrompt = "A cobbler's daughter weaves shoes made of moonlight that let anyone dance without ever getting tired.",
            recommendedAge = "8–10 years",
            recommendedTone = "Magical",
            artStyle = "Storybook",
            pageCount = 12,
            colorHex = 0xFF9333EA
        )
    )

    fun getRandomIdea(): StoryIdeaItem {
        return STORY_IDEAS.random()
    }
}
