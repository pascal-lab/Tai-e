import os
import sys


def countLine(filePath):
	f = open(filePath, encoding='utf-8')
	l = len(f.readlines())
	f.close()
	return l

def notExcluded(path, excludes):
	for exc in excludes:
		if exc in path:
			return False
	else:
		return True

if __name__ == '__main__':
	excludes = sys.argv[1:] + [ 'test', 'Test', ]
	total = 0
	nFile = 0
	for root, dirs, files in os.walk(os.getcwd()):
		for f in files:
			path = os.path.join(root, f)
			ext = os.path.splitext(path)[1]
			if ext == '.java':
				if notExcluded(path, excludes):
					l = countLine(path)
					print('%s: %d' % (path, l))
					total += l
					nFile += 1
	print('%d file, total line number: %d' % (nFile, total))
