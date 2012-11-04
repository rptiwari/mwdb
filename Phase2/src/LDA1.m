function [ WPALL,DPALL,ZALL ] = LDA1(WS,DS,T,WO)


%Set the number of topics
[w1,w2] = size(WS)

%Set the hyperparameters
ALPHA = 50 / T
BETA = 200 / w2
%BETA=0.01;
%ALPHA=8/T;

%The number of iterations

N = 180;

%The random seed

SEED = 3;

% What output to show (0=no output; 1=iterations; 2=all output)
% use 1 for debugging
OUTPUT = 2;

[ WP,DP,Z ] = GibbsSamplerLDA( WS , DS , 5 , N , ALPHA , BETA , SEED , OUTPUT );




WPALL = full(WP)
DPALL = full(DP)
ZALL = full(Z)




%[WO] = textread('words.mat','%s')

[S] = WriteTopics( WP , BETA , WO , 7 , 0.7 );

fprintf( '\n\nMost likely words in the first 5 topics:\n' );

S( 1:5 )
WriteTopics( WP , BETA , WO , 10 , 1.0 , 5 , 'topics.txt');

% [ Order ] = OrderTopics( DP , ALPHA );

% Order(1 : 5)
end