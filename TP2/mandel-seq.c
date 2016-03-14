/* ------------------------------
   $Id: mandel-seq.c,v 1.2 2008/03/04 09:52:55 marquet Exp $
   ------------------------------------------------------------
   essayer de recouvrir les communications
   Affichage de l'ensemble de Mandelbrot.
   Version sequentielle.
   
*/

#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <getopt.h>
#include "mpi.h"

/* Valeur par defaut des parametres */
#define N_ITER  255		/* nombre d'iterations */

#define X_MIN   -1.78		/* ensemble de Mandelbrot */
#define X_MAX   0.78
#define Y_MIN   -0.961
#define Y_MAX   0.961

#define X_SIZE  2048		/* dimension image */
#define Y_SIZE  1536
#define FILENAME "mandel.ppm"	/* image resultat */


/*------------------------------
  Les processus
  ------------------------------------------------------------*/
int self;			/* mon rang parmi les processus */
int procs;			/* nombre de processus */
int rnbr, lnbr;			/* mes voisins de droite et de gauche */

typedef struct {  
    int x_size, y_size;		/* dimensions */
    char *pixels;		/* matrice linearisee de pixels */
} picture_t;

static void 
usage() 
{
    fprintf(stderr, "usage : ./mandel [options]\n\n");
    fprintf(stderr, "Options \t Signification \t\t Val. defaut\n\n");
    fprintf(stderr, "-n \t\t Nbre iter. \t\t %d\n", N_ITER);
    fprintf(stderr, "-b \t\t Bornes \t\t %f %f %f %f\n",
	    X_MIN, X_MAX, Y_MIN, Y_MAX);
    fprintf(stderr, "-d \t\t Dimensions \t\t %d %d\n", X_SIZE, Y_SIZE);
    fprintf(stderr, "-f \t\t Fichier \t\t %s\n", FILENAME);

    exit(EXIT_FAILURE);
}

static void 
parse_argv (int argc, char *argv[], 
	    int *n_iter, 
	    double *x_min, double *x_max, double *y_min, double *y_max, 
	    int *x_size, int *y_size, 
	    char **path) 
{
    const char *opt = "b:d:n:f:";
    int c;
    
    /* Valeurs par defaut */
    *n_iter = N_ITER;
    *x_min  = X_MIN;
    *x_max  = X_MAX;
    *y_min  = Y_MIN;
    *y_max  = Y_MAX;
    *x_size = X_SIZE;
    *y_size = Y_SIZE;
    *path   = FILENAME;

    /* Analyse arguments */
    while ((c = getopt(argc, argv, opt)) != EOF) {    
	switch (c) {      
	    case 'b': 		/* domaine */
		sscanf(optarg, "%lf", x_min);
		sscanf(argv[optind++], "%lf", x_max);
		sscanf(argv[optind++], "%lf", y_min);
		sscanf(argv[optind++], "%lf", y_max);
		break;
	    case 'd':		/* largeur hauteur */
		sscanf(optarg, "%d", x_size);
		sscanf(argv[optind++], "%d", y_size);
		break;
	    case 'n':		/* nombre d'iterations */
		*n_iter = atoi(optarg);
		break;
	    case 'f':		/* fichier de sortie */
		*path = optarg;
		break;
	    default:
		usage();
	}
    }  
}

static void 
init_picture (picture_t *pict, int x_size, int y_size)
{  
    pict->y_size = y_size;
    pict->x_size = x_size;
    pict->pixels = malloc(y_size * x_size); /* allocation espace memoire */
} 

/* Enregistrement de l'image au format ASCII .ppm */
static void 
save_picture (const picture_t *pict, const char *pathname)
{  
    unsigned i;
    FILE *f = fopen(pathname, "w");  

    fprintf(f, "P6\n%d %d\n255\n", pict->x_size, pict->y_size); 
    for (i = 0 ; i < pict->x_size * pict->y_size; i++) {
	char c = pict->pixels[i];
	fprintf(f, "%c%c%c", c, c, c); /* monochrome blanc */
    }
    
    fclose (f);
}

static void
compute (picture_t *pict,
	 int nb_iter,
	 double x_min, double x_max, double y_min, double y_max)
{  
    int pos = 0;
    int iy, ix, i;
    double pasx = (x_max - x_min) / pict->x_size, /* discretisation */
	   pasy = (y_max - y_min) / pict->y_size; 

    /* Calcul en chaque point de l'image */
    for (iy = 0 ; iy < pict->y_size ; iy++) {
	    for (ix = 0 ; ix < pict->x_size; ix++) {
        double a = x_min + ix * pasx,
		    b = y_max - iy * pasy,
		    x = 0, y = 0;      
        for (i = 0 ; i < nb_iter ; i++) {
	        double tmp = x;
	        x = x * x - y * y + a;
	        y = 2 * tmp * y + b;
	        if (x * x + y * y > 4) /* divergence ! */
	            break; 
        }
        pict->pixels[pos++] = (double) i / nb_iter * 255;    
	    }
    }
}

int 
main (int argc, char *argv[]) 
{
    MPI_Comm com;	
    MPI_Status status;
    com = MPI_COMM_WORLD;
    MPI_Init (&argc, &argv);
    MPI_Comm_size (com, &procs);
    MPI_Comm_rank (com, &self);
    int n_iter,x_size, y_size,workcharge,division,dimension;		/* & dimensions de l'image */  
    double x_min, x_max, y_min, y_max,delta; /* bornes de la representation */
    char *pathname;		/* fichier destination */
    picture_t pict;
    picture_t mypict;
    parse_argv(argc, argv,&n_iter,&x_min, &x_max, &y_min, &y_max,&x_size, &y_size, &pathname);
    division = 2;
    dimension = x_size*y_size;
    if(self == 0) {
      init_picture(&pict, x_size, y_size);
    }
    /* calcul de la charge de travail = dimension de l'image  (donc sous dimensions / procs)*/
    workcharge = x_size * y_size / (procs * division);
    /* sous dimensions * nbprocs */
    chargeperproc = workcharge * procs;

    delta = ((y_max -y_min)/procs);

    init_picture(&mypict,x_size,y_size/procs);
    while(chargeperproc < dimension) {
      compute(&mypict,n_iter,x_min,x_max,y_min + chargeperproc,
    }
    compute (&mypict, n_iter, x_min, x_max, y_min + delta*(self+1), y_min + delta*(self));
    MPI_Gather(mypict.pixels,x_size*(y_size/procs),MPI_CHAR,pict.pixels,x_size*(y_size/procs),MPI_CHAR,0,com);

    if(self == 0) {
      save_picture(&pict,pathname);
    }
    MPI_Finalize ();
    exit(EXIT_SUCCESS);
}
