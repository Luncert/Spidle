
import sys

result = 0
for line in sys.stdin:
    i = int(line.strip())
    result += i
print(result)