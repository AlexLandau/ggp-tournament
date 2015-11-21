Work-in-progress library for tournament format specification and deterministic stateless scheduling

No API guarantees yet. Eventually (at version 1.0.0) the contents of the api package and the file format will be considered API; users should restrict themselves to those classes and methods. (Note that the current contents of the api package may be moved out and become non-API when the actual API is established.)

Current priorities include deterministic behavior and concurrency-correctness. Also, the supported tournament formats should be agnostic about the number of players that will be participating, to the extent possible; the intent is to allow a tournament file to be crafted, validated, and uploaded well in advance of the registration deadline. Another priority is compatibility with the distributed Tiltyard system, which encourages the use of a stateless library. 

Validation of tournament specs is medium-priority, as is good test coverage and fuzz testing of desired invariants.

Current non-priorities include performance at scale. This will be revisited when the system has proven useful and the API is stable.

Open question: Should we allow Java builders for tournament specs, or just the YAML format? This increases the API size substantially, but forcing people to go through JSON or YAML export to get back a Java object seems perverse, in case this becomes an actual use case.

Explanation of tournament specifications:

A tournament specification is a way of defining what matches should be run in a tournament and how they should result in a final set of standings of the players involved.

Stages: Each tournament involves some number of stages; this number is usually expected to be small, like 1 or 2. Each stage can have its own format, and the outcome of one stage is used as the seeding for the next stage. In addition, there may be cutoffs for players

For example, a tournament may have a Swiss-style stage used to select the top four competitors that then compete in a single-elimination stage.

Rounds and matches: Stages then have some number of rounds, each of which has some number of matches. The exact way these stages are used varies by format, though in general each round is interpreted as corresponding to one grouping of players. Not every match specified may end up being played. For example, a single-elimination format with three matches in a round will interpret it as a best-of-three competition, and in particular will end once one player has at least 151 points and more than their opponent.

Games: Games are defined once in the specification and then specified elsewhere by their name. These are specified with a ggp.org-style repository location and game key.

Some properties of games, such as the number of players involved and whether they are fixed-sum, must be specified. This helps ensure that tournament formats handle these games appropriately, or flags where they are being used inappropriately (such as a non-fixed-sum game being used in a single-elimination round).

A note on the fixed-sum property (which should be better integrated into documentation elsewhere): The meaning of this is pretty obvious when used with two-player games. However, the Swiss format treats it differently; the goal here is to ensure that the way players are matched is sensible, so it is used differently from its literal definition.

Effectively, games with the possibility of a king-making element can have very random outcomes and correspond only weakly with player skill. We prefer to treat these more like game-theoretical games, where playing against a variety of opponents in a large number of matches gives a better sense of performance.

Instead, the fixed-sum label is reserved for games where if an opponent wants to reduce your score, the only way to do so is to increase their own quality of play in such a way that it also increases their own score. This is a definition that also applies to two-player fixed-sum games. It rules out king-making, in which a player could decide to play in such a way to grant an advantage to one opponent over another without an improved understanding of the game. It also preserves the fact that a better opponent (as measured by score in previous games) is an opponent that will provide more information about a player's own abilities. By contrast, in two king-making-defined matches, the stronger opponent may well make decisions that favor you more than a weaker opponent.