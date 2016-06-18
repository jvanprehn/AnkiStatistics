# -*- coding: utf-8 -*-
# Name: Forecast Statistics Simulation
# Version: 0.1
# Author: Jeffrey van Prehn (based on https://gist.github.com/gregreen/934e3cf8b733f23f7b32)
# License: GNU GPL, version 3 or later; http://www.gnu.org/copyleft/gpl.html
#
# This add-on draws an additional graph in Anki's "stats" window,
# which is similar to the 'Forecast' graph, but also takes 
# the effect of future reviews into account.

from anki import stats
from anki.hooks import wrap
from aqt.utils import showInfo
import time, random, re, sys

CARD_TYPE_NEW = 0
CARD_TYPE_YOUNG = 1
CARD_TYPE_MATURE = 2

now = time.mktime(time.localtime())

#A review has a particular outcome with a particular probability.
#A review results in the state of the card (card interval) being changed.
#A ReviewOutcome bundles the probability of the outcome and the card with changed state.
class ReviewOutcome:

        def __init__(self, card, prob):
            self.__card = card
            self.__prob = prob

        def setAll(self, card, prob):
            self.__card = card
            self.__prob = prob

        def getCard(self):
            return self.__card

        def getProb(self):
            return __prob;

        def __repr__():
            return "ReviewOutcome(card=%r,prob=%r)" % (self.__card,self.__prob)

class EaseClassifier:

    # Prior that half of new cards are answered correctly
    priorNew    = [5, 0, 5, 0]		#half of new cards are answered correctly
    priorYoung  = [1, 0, 9, 0]		#90% of young cards get "good" response
    priorMature = [1, 0, 9, 0]		#90% of mature cards get "good" response

    #hard Doesn't occur in query_new
    queryBaseNew = '''
        select
          count() as N,
          sum(case when ease=1 then 1 else 0 end) as repeat,
          0 as hard,
          sum(case when ease=2 then 1 else 0 end) as good,
          sum(case when ease=3 then 1 else 0 end) as easy
        from revlog
        '''

    queryBaseYoungMature = '''
        select
          count() as N,
          sum(case when ease=1 then 1 else 0 end) as repeat,
          sum(case when ease=2 then 1 else 0 end) as hard,
          sum(case when ease=3 then 1 else 0 end) as good,
          sum(case when ease=4 then 1 else 0 end) as easy
        from revlog
        '''

    queryNew    = queryBaseNew +         '''where type=0;'''
    queryYoung  = queryBaseYoungMature + '''where type=1 and lastIvl < 21;'''
    queryMature = queryBaseYoungMature + '''where type=1 and lastIvl >= 21;'''

    def __init__(self, parent):
        self.__db = parent.col.db
        self.__probabilities = None
        self.__probabilitiesCumulative = None
        self.calculateCumProbabilitiesForNewEasePerCurrentEase()
	
    def calculateCumProbabilitiesForNewEasePerCurrentEase(self):
        query_new = '''
            select
              count() as N,
              sum(case when ease=1 then 1 else 0 end) as repeat,
              sum(case when ease=2 then 1 else 0 end) as good,
              sum(case when ease=3 then 1 else 0 end) as easy
            from revlog
            where type=0
        '''

        query_young = '''
            select
              count() as N,
              sum(case when ease=1 then 1 else 0 end) as repeat,
              sum(case when ease=2 then 1 else 0 end) as hard,
              sum(case when ease=3 then 1 else 0 end) as good,
              sum(case when ease=4 then 1 else 0 end) as easy
            from revlog
            where type=1 and lastIvl < 21
        '''
	
        query_mature = '''
            select
              count() as N,
              sum(case when ease=1 then 1 else 0 end) as repeat,
              sum(case when ease=2 then 1 else 0 end) as hard,
              sum(case when ease=3 then 1 else 0 end) as good,
              sum(case when ease=4 then 1 else 0 end) as easy
            from revlog
            where type=1 and lastIvl >= 21
        '''

        new_prob = [float(x) for x in self.__db.all(query_new)[0]] 
        # Add in some fake reviews. Makes outcome probabilities reasonable when there are
        # very few reviews in the review log. This is essentially a prior.
        new_prob[0] += 10  # Prior that half of new cards are answered correctly
        new_prob[1] += 5
        new_prob[2] += 5
        new_prob = [x / sum(new_prob[1:]) for x in new_prob[1:]]
        new_cmf = [sum(new_prob[:i+1]) for i in range(len(new_prob))]
        
        young_prob = [float(x) for x in self.__db.all(query_young)[0]]
        young_prob[0] += 10  # Prior that 90% of young cards get "good" resonse
        young_prob[1] += 1
        young_prob[3] += 9
        young_prob = [x / sum(young_prob[1:]) for x in young_prob[1:]]
        young_cmf = [sum(young_prob[:i+1]) for i in range(len(young_prob))]
	    
        mature_prob = [float(x) for x in self.__db.all(query_mature)[0]]
        mature_prob[0] += 10  # Prior that 90% of mature cards get "good" resonse
        mature_prob[1] += 1
        mature_prob[3] += 9
        mature_prob = [x / sum(mature_prob[1:]) for x in mature_prob[1:]]
        mature_cmf = [sum(mature_prob[:i+1]) for i in range(len(mature_prob))]
	    
        outcome_cmf = [new_cmf, young_cmf, mature_cmf]
        
        return outcome_cmf
	
class CardType:
    def __init__(self, ivl, ctype, due, factor=2.5):
        self._ivl = ivl
        self._ctype = ctype
        self._due = due
        self._factor = factor

class Card:
    def __init__(self, id, ivl, factor, due, correct, lastReview):
        self.__id = id
        self.__ivl = ivl
        self.__factor = factor / 1000.0
        self.__due = due
        self.__correct = correct
        self.__lastReview = lastReview

    def __repr__():
        return "Card(id=%r,ivl=%r,factor=%r,due=%r,correct=%r,lastReview=%r)" % (self.__id,self.__ivl,self.__factor*1000.0,self.__due,self.__correct,self.__lastReview)

    def setAll(self, id, ivl, factor, due, correct, lastReview):
        self.__id = id
        self.__ivl = ivl
        self.__factor = factor / 1000.0
        self.__due = due
        self.__correct = correct
        self.__lastReview = lastReview

    def setAll(self, card):
        self.__id = card.__id
        self.__ivl = card.__ivl
        self.__factor = card.__factor
        self.__due = card.__due
        self.__correct = card.__correct
        self.__lastReview = card.__lastReview

    def getId(self):
        return self.__id

    def getIvl(self):
        return self.__ivl

    def setIvl(self, ivl):
        self.__ivl = ivl

    def getFactor(self):
        return self.__factor

    def setFactor(self, factor):
        self.__factor = factor

    def getDue(self):
        return self.__due

    def setDue(self, due):
        self.__due = due

    # Type of the card, based on the interval.
    # @return CARD_TYPE_NEW if interval = 0, CARD_TYPE_YOUNG if interval 1-20, CARD_TYPE_MATURE if interval >= 20
    def getType(self):
        if self.__ivl == 0:
            return CARD_TYPE_NEW
        elif self.__ivl >= 21:
            return CARD_TYPE_MATURE
        else:
            return CARD_TYPE_YOUNG

    def getCorrect(self):
        return __correct

    def setCorrect(self, correct):
        self.__correct = correct

    def getLastReview(self):
        return self.__lastReview

    def setLastReview(self, lastReview):
        self.__lastReview = lastReview

class DeckFactory:

    def __init__(self, parent):
        self.__decks = parent.col.decks

    def createDeck(self, did, decks):
        # Timber.d("Trying to get deck settings for deck with id=" + did);

        conf = self.__decks.confForDid(did)

        newPerDay = Settings.getMaxNewPerDay()
        revPerDay = Settings.getMaxReviewsPerDay()
        initialFactor = Settings.getInitialFactor()

        if not conf['dyn']:
            conf = self.__decks.confForDid(did)
				
            revPerDay = conf['rev']['perDay']
            newPerDay = conf['new']['perDay']
            initialFactor = conf['new']['initialFactor']

            # Timber.d("rev.perDay=" + revPerDay);
            # Timber.d("new.perDay=" + newPerDay);
            # Timber.d("new.initialFactor=" + initialFactor);

        return Deck(did, newPerDay, revPerDay, initialFactor)

class Deck:

    def __init__(self, did, newPerDay, revPerDay, initialFactor):
        self.__did = did
        self.__newPerDay = newPerDay
        self.__revPerDay = revPerDay
        self.__initialFactor = initialFactor

    def getDid(self):
        return self.__did

    def getNewPerDay(self):
        return self.__newPerDay

    def getRevPerDay(self):
        return self.__revPerDay

    def getInitialFactor(self):
        return self.__initialFactor

class CardIterator:

    def __init__(self, db, today, deck):
        self.__today = today
        self.__deck = deck

        did = deck.getDid()
        query = '''
                SELECT id, due, ivl, factor, type, reps
                FROM cards
                WHERE did IN (?)
                order by id
                '''
        # Timber.d("Forecast query: %s", query);
        arg_did = (did,)
        self.__cur = db.execute(query, arg_did)
		
    def moveToNext(self):

        self.__row = self.__cur.fetchone()
        
        if self.__row == None:
            return false
        else:
            return true

    def current(self, card):
        card.setAll(self.__row[0],                                                          # Id
                    0 if self.__row[5] == 0 else self.__row[2],  		                    # reps = 0 ? 0 : card interval
                    self.__row[3] if self.__row[3] > 0 else self.__deck.getInitialFactor(), # factor
                    max(self.__row[1] - self.__today, 0),                                   # due
                    1,                                                                      # correct
                    -1                                                                      # lastreview
        )

    def close (self):

        #if (cur != None && !cur.isClosed())
        if (cur is not None):
            cur.close()

def draw_cmf(cmf):
    v = random.random()

    for i in range(0, len(cmf)):
        if(v <= cmf[i]):
		     return i;
    return len(cmf);

def sim_single_review(outcome_cmf, c_type, c_ivl, c_factor):
    outcome = draw_cmf(outcome_cmf[c_type])
    
    if c_type == 0:
        if outcome <= 1:
            c_ivl = 1
        elif outcome == 2:
            c_ivl = 4
        else:
            raise ValueError('Outcome should be <= 2 for new cards.')
    elif c_type <= 2:
        if outcome == 0:
            c_ivl = 1
        elif outcome == 1:
            c_ivl *= 1.2
        elif outcome == 2:
            c_ivl *= 1.2 * c_factor
        elif outcome == 3:
            c_ivl *= 1.2 * 2. * c_factor
        else:
            raise ValueError('Outcome should be <= 3 for young/mature cards.')
    else:
        raise ValueError('Card type should be <= 2.')
    
    c_type = (1 if c_ivl <= 21 else 2)
    correct = (outcome > 0)
    
    return c_type, c_ivl, correct	
	
def calc_ctype(reps, ivl):
    if reps == 0:
        return 0
    elif ivl <= 21:
        return 1
    else:
        return 2

def forecast_n_reviews(outcome_cmf, cards, n_time_bins=30, time_bin_length=1, max_add_per_day=5, n_smooth=1):
    t_max = n_time_bins * time_bin_length

    # Forecasted number of reviews
    #   0 = Learn
    #   1 = Young
    #   2 = Mature
    #   3 = Relearn
    n_reviews = [[0 for i in xrange(n_time_bins)] for j in xrange(4)]
    
    # Forecasted final state of deck
    n_cards = len(cards)
    n_in_state = [float(0) for i in xrange(4)]
    
    for k in xrange(n_smooth):
        n_added_today = 0
        t_add = 0
        
        for l,card in enumerate(cards):
            # Initiate time to next due date of card
            t_elapsed = card._due
            
            # Rate-limit new cards by shifting starting time
            if card._ctype == 0:
                n_added_today += 1
                t_elapsed += t_add
                if n_added_today >= max_add_per_day:
                    t_add += 1
                    n_added_today = 0
                    
            c_ivl = card._ivl    # card interval
            c_type = card._ctype  # 0=new, 1=learn, 2=mature
    
            # Simulate reviews
            while t_elapsed < t_max:
                # Update the forecasted number of reviews
                n_reviews[c_type][int(t_elapsed / time_bin_length)] += 1
    
                # Simulate response
                c_type, c_ivl, correct = sim_single_review(outcome_cmf, c_type, c_ivl, card._factor)
    
                # If card failed, update "relearn" count
                if correct == 0:
                    n_reviews[3][int(t_elapsed / time_bin_length)] += 1
    
                # Advance time to next review
                t_elapsed += c_ivl
            
            n_in_state[c_type] += 1
	
	for j in xrange(n_time_bins):
	    for i in xrange(4):
		    n_reviews[i][j] /= float(n_smooth)
	
    return (n_reviews,                                  # Number of reviews in each time bin
            [x / float(n_smooth) for x in n_in_state])  # Number of cards in each state (new, young, mature, relearn)
		
def forecastSimulatePDFHistory(data, chunks=None, chunk_size=1):
    #Compute history
    if not chunks:
        try:
            chunks=max(data)/chunk_size+1 #nb of periods to look back
        except:
            chunks = 1 #This happens if the deck is empty
    histogram = [0]*(chunks+1)
    cumul=[]
    delta=[]
    subtotal=0
    date=-chunks
    #Fill histogram, as a list. d = nb of days in the past (0=today).
    for d in data:
        if d <= chunks*chunk_size:
            histogram[d/chunk_size] += 1
        else:
            subtotal+=1
    #Fill history, as a list of coordinates: [(relative_day, nb_values),...]
    while len(histogram):
        v=histogram.pop()
        subtotal +=v
        cumul.append((date, subtotal))
        delta.append((date, v))
        date+=1
    return cumul, delta	
	
##################################################
def progressForecastGraphUsingSimulation(self, chunks, chunk_size, chunk_name):
    
    easeClassifier = EaseClassifier(self)
    outcome_cmf = easeClassifier.calculateCumProbabilitiesForNewEasePerCurrentEase()

    query_cards = '''
        select
          reps,
          due,
          ivl,
          factor
        from cards
    '''
    #where did=1383666618882
    
    cards = [CardType(ivl, calc_ctype(reps, ivl), due, factor=(factor/1000. if factor > 0 else 2.5)) for reps,due,ivl,factor in self.col.db.all(query_cards)]
    
    # Fix due dates
    t_due = [card._due for card in cards]
    ctype = [card._ctype for card in cards]
    #t_today = min(t_due[ctype != 0])
    t_today = min([t_due[i] for i in range(0, len(t_due)) if ctype[i] != 0])
	
    for card in cards:
        card._due -= t_today
        if card._ctype == 0:
            card._due = 0
	
	time_bin_length = 1
	n_time_bins = 30

	n_reviews, final_ctype = forecast_n_reviews(
		outcome_cmf,
		cards,
		n_time_bins=n_time_bins,
		time_bin_length=time_bin_length,
		max_add_per_day=15,
		n_smooth=1
	)
	
    #Draw graph
	txt=""
    txt += self._title(
        _("Progress graph"),
        _("The number of Anki cards & notes you have learned over time"))

    # Forecasted number of reviews
    #   0 = Learn
    #   1 = Young
    #   2 = Mature
    #   3 = Relearn
		
    colYoung = "#7c7"
    colMature = "#070"
    colLearn = "#00F"
    colRelearn = "#c00"		
		
    data = [
        dict(data=[(i, n_reviews[0][i]) for i in range(len(n_reviews[0]))], color=colLearn, yaxis=2, bars={'show':True}, lines={"show":False }, stack=True, label=_("Learn")),
        dict(data=[], stack=False), #Workaround for some weird graph staking issue
        dict(data=[(i, n_reviews[1][i]) for i in range(len(n_reviews[1]))], color=colYoung, yaxis=2, bars={'show': True}, lines={"show":False}, stack=True, label=_("Young")), 
        dict(data=[], stack=False),
        dict(data=[(i, n_reviews[2][i]) for i in range(len(n_reviews[2]))], color=colMature, yaxis=2, bars={'show': True}, lines={"show":False}, stack=True, label=_("Mature")),
		dict(data=[], stack=False),
        dict(data=[(i, n_reviews[3][i]) for i in range(len(n_reviews[3]))], color=colRelearn, yaxis=2, bars={'show': True}, lines={"show":False}, stack=True, label=_("Relearn"))
        ]
 
    #txt += str(data)

    txt += self._graph(id="forecast_simulate_graph", 
	                   data=data,
                       ylabel = "New cards per "+chunk_name, 
                       ylabel2=_("Cumulative notes"), 
                       conf=dict(
                                 xaxis=dict(tickDecimals=0), 
								 yaxes=[dict(tickDecimals=0, position="right")]
								)
					   )
    # txt += "<div>Total known today: <b>%d cards, %d notes</b></div>" %(len(cards), len(notes))
    return txt

def myForecastStats(self, _old):
    if self.type == 0:
        chunks = 30; chunk_size = 1; chunk_name="day"
    elif self.type == 1:
        chunks = 52; chunk_size = 7; chunk_name="week"
    else:
        chunks = None; chunk_size = 30; chunk_name="month"
    txt = _old(self)
    txt+= progressForecastGraphUsingSimulation(self, chunks, chunk_size, chunk_name)
    return txt

try:
    stats.CollectionStats.todayStats = wrap(stats.CollectionStats.todayStats, myForecastStats, "around")
except AttributeError:
    #Happens on Anki 2.0.0, fixed at least in 2.0.14
    showInfo("The Progress Graph add-on is incompatible with your version of Anki.<br>Please upgrade to the latest version.<br>If the problem persists, please contact the author (<tt>tools -> add-ons -> Browse&install -> Browse -> Progress Graph -> Info -> Ask a question</tt>).")
    pass

