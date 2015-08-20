Work-in-progress library for tournament format specification and deterministic stateless scheduling

No API guarantees yet. Eventually (at version 1.0.0) the contents of the api package and the file format will be considered API; users should restrict themselves to those classes and methods. (Note that the current contents of the api package may be moved out and become non-API when the actual API is established.)

Current priorities include deterministic behavior and concurrency-correctness. Also, the supported tournament formats should be agnostic about the number of players that will be participating, to the extent possible; the intent is to allow a tournament file to be crafted, validated, and uploaded well in advance of the registration deadline. Another priority is compatibility with the distributed Tiltyard system, which encourages the use of a stateless library. 

Validation of tournament specs is medium-priority, as is good test coverage and fuzz testing of desired invariants.

Current non-priorities include performance at scale. This will be revisited when the system has proven useful and the API is stable.

Open question: Should we allow Java builders for tournament specs, or just the YAML format? This increases the API size substantially, but forcing people to go through JSON or YAML export to get back a Java object seems perverse, in case this becomes an actual use case.