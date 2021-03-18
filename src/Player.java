import java.util.*;


/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {

    static SplittableRandom R = new SplittableRandom(0);
    static final String dirs[] = new String[]{"U","R","D","L"};


/*
static
{
    // notification listener. is notified whenever a gc finishes.
    NotificationListener notificationListener = new NotificationListener()
    {
        @Override
        public void handleNotification(Notification notification,Object handback)
        {
            if (notification.getType().equals(com.sun.management.GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION))
            {
                System.err.println("GC"+notification);
            }
        }
    };

    // register our listener with all gc beans
    for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans())
    {
        NotificationEmitter emitter = (NotificationEmitter) gcBean;
        emitter.addNotificationListener(notificationListener,null,null);
    }
}
*/
    static List<Board> toExplore = new ArrayList<>(200);
    static List<Board> toExploreNext = new ArrayList<>(200);

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        Board board = new Board();
        board.seed = 290797;
        board.spawnTile();
        board.spawnTile();
        Arrays.asList(board.grid).forEach(e -> System.err.println(Arrays.toString(e)));

        Map<Integer, Byte> logs = new HashMap<>();
        logs.put(0, (byte) 0);
        for(byte i=1; i<17;i++)
            logs.put(1<<i, i);

        long start = System.nanoTime();

            toExplore.add(board);
            int toExploreIndex=0;
            int toExploreMax=1;

            Board best=null;
            float bestEval=0;

            int iter=0;
            long timeout = 10_000_000_000L;

            while(System.nanoTime() - start < timeout && toExploreIndex<toExploreMax) {

                while(System.nanoTime() - start < timeout && toExploreIndex<toExploreMax) {
                    
                    Board current = toExplore.get(toExploreIndex++);
                    if(current != board)
                        current.spawnTile();

                    for(int dir=0; dir<4; dir++) {
                        
                        Board b = new Board();

                        b.set(current);

                        if(b.applyMove(dir)) {

                            b.moves.add(dirs[dir]);

                            toExploreNext.add(b);

                        }

                    }
    
                    iter++;
                }

                if(System.nanoTime() - start > timeout)
                    break;

                List<Board> tmp = toExplore;
                toExplore = toExploreNext;
                toExploreNext = tmp;
                
                toExplore.sort((a,b) -> -Float.compare(a.eval,b.eval));
                toExploreIndex=0;
                toExploreMax = Math.min(toExplore.size(), 150);
                if(toExploreMax == 150) {
                    for (int i = 0; i < 15; i++) {
                        int p = toExploreMax + R.nextInt(toExplore.size()-toExploreMax);
                        toExplore.set(toExploreMax-i-1, toExplore.get(p));
                    }
                }
 
                toExploreNext.clear();

                if(!toExplore.isEmpty()) {
                float eval = toExplore.get(0).eval;
                if(best==null || bestEval<eval) {
                    best=toExplore.get(0);
                    bestEval=eval;
                }
                }

//                System.err.println(toExploreMax + " "+ eval);

            }

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");

            //System.err.println(iter+" "+best.score);
            System.out.println(best.moves.stream().reduce("",(a,v)-> a+=v));
            Arrays.asList(best.grid).forEach(e -> System.err.println(Arrays.toString(e)));
            System.out.println(best.score);

    }
}

class Board {
    static final int SIZE = 4;
    static final byte[][] transfos = new byte[17*17*17*17][4];
    static final int[] scores = new int[17*17*17*17];
    static final ArrayList<Integer> freeCells = new ArrayList<>();

    static int encode(int a, int b, int c, int d) {
        return a + b*17 + c*17*17 + d*17*17*17; 
    }

    static {
        long start = System.currentTimeMillis();
        for(byte a=0;a<17;a++) {
            for(byte b=0;b<17;b++) {
                for(byte c=0;c<17;c++) {
                    for(byte d=0;d<17;d++) {
                        byte[] toArr={a,b,c,d};
                        
                        int score = Board.applyMove(toArr);

                        int from=encode(a,b,c,d);
                        int to=encode(toArr[0],toArr[1],toArr[2],toArr[3]);
                        transfos[from]=toArr;
                        
                        scores[from]= to==from ?-1:score;
                    }                    
                }
            }
        }
        //System.err.println(Arrays.toString(transfos.get("-2-2-2-2")));
        //System.err.println(Arrays.toString(transfos.get("-2-4-8-2")));
        //System.err.println(Arrays.toString(transfos.get("-2048-2048-2-2")));

  //      System.err.println((System.currentTimeMillis()-start));
    }

    final byte[][] grid = new byte[SIZE][SIZE];
    int score=0;
    float eval;
    long seed;
    List<String> moves=new ArrayList<>();

    Board() {
    }

    void set(Board b) {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {         
                grid[x][y]=b.grid[x][y];
            }
        }
        score=b.score;
        seed=b.seed;
        moves.clear();
        moves.addAll(b.moves);
    }

    void spawnTile() {
         freeCells.clear();
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                if (grid[x][y] == 0) freeCells.add(x + y * SIZE);
            }
        }
        if(freeCells.size()>0) {
            int spawnIndex = freeCells.get((int) seed % freeCells.size());
            int value = (seed & 0x10) == 0 ? 1 : 2;

            grid[spawnIndex % SIZE][spawnIndex / SIZE] = (byte) value;

            seed = seed * seed % 50515093L;
        }
    }

     boolean applyMove(int dir) {
        int turnScore = 0;
        boolean changed=false;

        if(dir==1) {//R
            for (int i = 0; i < SIZE; i++) {
                int from = 0;
                for(int j=0,p=1; j<SIZE;j++,p*=17) {
                    from += grid[j][i]*p;
                }
                
                if(scores[from]>=0) {
                    changed=true;
                    byte[] to = transfos[from];
                    turnScore += scores[from];
                    for(int j=0; j<SIZE;j++) {
                        grid[j][i] = to[j];
                    }
                }
            }
        }
        if(dir==3) {//L
            for (int i = 0; i < SIZE; i++) {
                int from = 0;
                for(int j=0,p=1; j<SIZE;j++,p*=17) {
                    from += grid[SIZE-1-j][i]*p;
                }
                if(scores[from]>=0) {
                    changed=true;
                    byte[] to = transfos[from];
                    turnScore += scores[from];
                    for(int j=0; j<SIZE;j++) {
                        grid[j][i] = to[SIZE-1-j];
                    }
                }
            }
        }
        if(dir==0) {//U
            for (int i = 0; i < SIZE; i++) {
                int from = 0;
                for(int j=0,p=1; j<SIZE;j++,p*=17) {
                    from += grid[i][SIZE-1-j]*p;
                }
                if(scores[from]>=0) {
                    changed=true;
              
                    byte[] to = transfos[from];
                    turnScore += scores[from];
                    for(int j=0; j<SIZE;j++) {
                        grid[i][j] = to[SIZE-1-j];
                    }
                }
            }
        }
        if(dir==2) {//D
            for (int i = 0; i < SIZE; i++) {
                int from = 0;
                for(int j=0,p=1; j<SIZE;j++,p*=17) {
                    from += grid[i][j]*p;
                }
                if(scores[from]>=0) {
                    changed=true;

                    byte[] to = transfos[from];
                    turnScore += scores[from];
                    for(int j=0; j<SIZE;j++) {
                        grid[i][j] = to[j];
                    }
                }
            }
        }

        score += turnScore;

        eval = this.eval();

        return changed;
    }


     static int applyMove(byte[] to) {
        boolean[] merged = new boolean[SIZE];

        int turnScore = 0;
            int finalTarget = SIZE - 1 ;
            for (int j = 1; j < SIZE; j++) {
                int source = finalTarget - j;
                if (to[source] == 0) continue;
                for (int k = j - 1; k >= 0; k--) {
                    int intermediate = finalTarget - k;
                    if (to[intermediate] == 0) {
                        to[intermediate] = to[source];
                        to[source] = 0;
                        source = intermediate;
                    } else {
                        if (!merged[intermediate] && to[intermediate] == to[source]) {
                            to[source] = 0;
                            to[intermediate] ++;
                            merged[intermediate] = true;
                            turnScore += 1<<to[intermediate];
                        }
                        break;
                    }
                }
            }
        
        return turnScore;
    }


    static final List<Byte> valuesOrdered = new ArrayList<>();
    private float eval() {
        int free=0;
        float h=1;
        valuesOrdered.clear();
        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                valuesOrdered.add(grid[x][y]);
                if(grid[x][y]==0)
                    free++;
            }
        }
        valuesOrdered.sort((o1, o2) -> -Byte.compare(o1,o2));
        int err=0;
        int i=0;
        for (int x = 0; x < SIZE; x++) {
            err+=Math.abs(grid[x][0] - valuesOrdered.get(i++));
        }
        for (int x = 0; x < SIZE; x++) {
            err+=Math.abs(grid[SIZE-1-x][1] - valuesOrdered.get(i++));
        }
        for (int x = 0; x < SIZE; x++) {
            err+=Math.abs(grid[x][2] - valuesOrdered.get(i++));
        }
        for (int x = 0; x < SIZE; x++) {
            err+=Math.abs(grid[SIZE-1-x][3] - valuesOrdered.get(i++));
        }
        h*= (free);
        h*=1.0/(1+err);
        return score*h;
    }

}