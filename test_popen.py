from itertools import product
import subprocess
import time

def run_match_popen(pr):
    print("Running run_match_popen")
    outputs = []
    start_time = time.time()
    procs = []
    try:
        for _ in range(pr):
            proc = subprocess.Popen(["python", "helloworld.py"], stdout=subprocess.PIPE, stderr=subprocess.PIPE )
            procs.append(proc)
            print(time.time())
        for p in procs:
            print('herep: ', time.time())
            out, err = p.communicate()
        # procs = [subprocess.Popen(["python", "helloworld.py"], stdout=subprocess.PIPE) for _ in range(pr)]
        # for p in procs:
        #     out, err = p.communicate()
            # outputs.append(str(out))
    except subprocess.CalledProcessError as exc:
        print("Status: FAIL", exc.returncode, exc.output)
        return 'Error'
    else:
        print("--- %s seconds ---" % (time.time() - start_time))
        # print(outputs[0])
        # print(outputs[1])  


def run_match(pr): 
    print("Running run_match")
    start_time = time.time()
    try:
        for _ in range(pr):
        out = subprocess.check_output(["python", "helloworld.py"])
        print('here: ', time.time())
    except subprocess.CalledProcessError as exc:
        print("Status: FAIL", exc.returncode, exc.output)
        return 'Error'
    else:
        print("--- %s seconds ---" % (time.time() - start_time))
        # print(outputA)
        # print(outputB)

# Run matches
pr = 15
# run_match(pr)
run_match_popen(pr)