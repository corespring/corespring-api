# Results

## Single calls

* local server -> remote db

### 400 session ids (315 itemIds): 

| type | no cache   | cache | notes                  |
|------|------------|-------| -----------------------|
| one  | 101s       | 1.3s  | caching for 60 minutes |
| two  | 5s         | 1.2s  | caching for 1 minute   |

### 200 session ids (120 itemIds): 

| type | no cache   | cache | notes                  |
|------|------------|-------| -----------------------|
| one  | 54s        | 1.2s  | caching for 60 minutes |
| two  | 3.3s       | 1.0s  | caching for 1 minute   |

### 50 session ids (5 itemIds): 

| type | no cache   | cache | notes                  |
|------|------------|-------| -----------------------|
| one  | 7s         | 0.6s  | caching for 60 minutes |
| two  | 2s         | 0.6s  | caching for 1 minute   |

---

## Parallel calls

??