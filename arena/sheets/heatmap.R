library(readr)
library(pheatmap)
library(plyr)
library("dplyr")
library("tidyverse")

RARENA <- read_csv("/tmp/RARENA.csv")

to_matrix = function(tbl, rownames = NULL){
  
  tbl %>%
    {
      if(
        !tbl %>% 
        { if(!is.null(rownames)) (.) %>% dplyr::select(- contains(rownames)) else (.) } %>%
        dplyr::summarise_all(class) %>% 
        tidyr::gather(variable, class) %>%
        pull(class) %>% unique %>% identical("numeric")
      ) warning("to_matrix says: there are NON-numerical columns, the matrix will NOT be numerical")
      
      (.)
      
    } %>%
    as.data.frame() %>%
    { 
      if(!is.null(rownames)) 
        (.) %>% 
        magrittr::set_rownames(tbl %>% pull(!!rownames)) %>%
        dplyr::select(- !!rownames)
      else (.) 
    } %>%
    as.matrix()
}

num_arena <- RARENA %>% drop_na() %>% select(-S0_0, -S1_0) %>% mutate(ID = str_trunc(ID, 10, "right")) %>% mutate(IDD = paste(ID, Class, PermId, sep = "_")) %>% select("IDD", starts_with("S")) %>% mutate(across(starts_with("S"), function(X) { as.numeric(factor(X)) }))

mymatrix <- num_arena %>% as_tibble() %>% to_matrix(rownames = "IDD")
#mode(mymatrix) <- "numeric" # set to numeric isntead of string

# show heatmap
pheatmap(mymatrix, cluster_cols = F, cutree_rows = 2)

pheatmap(mymatrix, cluster_cols = F)

## print to pdf

pdf("heatmap.pdf", width=12, height=16)
pheatmap(mymatrix, cluster_cols = F)
dev.off()
