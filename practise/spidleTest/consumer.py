
import sys

result = 0
for line in sys.stdin:
    data = line[line.find(' ') + 1:]
    data = int(data)
    result += data
print(result)