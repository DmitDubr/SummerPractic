package api

import "time"

type ErrorResponse struct {
	Code    string              `json:"code"`
	Message string              `json:"message"`
	Details []ValidationDetail  `json:"details,omitempty"`
}

type ValidationDetail struct {
	Field   string `json:"field"`
	Message string `json:"message"`
}

type PaginationMeta struct {
	Total  int `json:"total"`
	Limit  int `json:"limit"`
	Offset int `json:"offset"`
}

type InstructorSummary struct {
	ID          string   `json:"id"`
	Name        string   `json:"name"`
	AvatarURL   *string  `json:"avatarUrl,omitempty"`
	PhotoURL    *string  `json:"photoUrl,omitempty"`
	Rating      *float64 `json:"rating"`
	RatingCount int      `json:"ratingCount,omitempty"`
}

type InstructorDetail struct {
	InstructorSummary
	PhotoURL    *string `json:"photoUrl,omitempty"`
	RatingCount int     `json:"ratingCount"`
}

type SlotSummary struct {
	ID         string            `json:"id"`
	StartAt    time.Time         `json:"startAt"`
	Format     string            `json:"format,omitempty"`
	Zone       string            `json:"zone,omitempty"`
	Instructor InstructorSummary `json:"instructor"`
	FreeSpots  int               `json:"freeSpots"`
	Capacity   int               `json:"capacity"`
	Price      *float64          `json:"price"`
	Status     string            `json:"status"`
	IsBookable bool              `json:"isBookable"`
}

type TrainingFormatInfo struct {
	ID    string `json:"id"`
	Name  string `json:"name"`
	Level string `json:"level"`
}

type ZoneInfo struct {
	ID   string `json:"id"`
	Name string `json:"name"`
}

type GymInfo struct {
	ID      string `json:"id,omitempty"`
	Name    string `json:"name"`
	Address string `json:"address"`
}

type RentalAvailability struct {
	ShoesAvailable   int  `json:"shoesAvailable"`
	HarnessAvailable int  `json:"harnessAvailable"`
	IsBookable       bool `json:"isBookable"`
}

type PriceBreakdown struct {
	TrainingPrice      float64 `json:"trainingPrice"`
	ShoesRentalPrice   float64 `json:"shoesRentalPrice"`
	HarnessRentalPrice float64 `json:"harnessRentalPrice"`
	TotalPrice         float64 `json:"totalPrice"`
}

type SlotDetail struct {
	SlotSummary
	EndAt              time.Time          `json:"endAt"`
	DurationMinutes    int                `json:"durationMinutes"`
	ZoneName           string             `json:"zoneName,omitempty"`
	FormatInfo         TrainingFormatInfo `json:"formatInfo,omitempty"`
	BasePrice          float64            `json:"basePrice"`
	RentalAvailable    bool               `json:"rentalAvailable"`
	RentalAvailability RentalAvailability `json:"rentalAvailability"`
	PriceBreakdown     PriceBreakdown     `json:"priceBreakdown"`
	Gym                GymInfo            `json:"gym"`
}

type SlotListResponse struct {
	Items []SlotSummary  `json:"items"`
	Meta  PaginationMeta `json:"meta,omitempty"`
}

type InstructorListResponse struct {
	Items []InstructorSummary `json:"items"`
}

type ClientContacts struct {
	Name  string `json:"name"`
	Phone string `json:"phone"`
}

type ClientProfile struct {
	ID              string `json:"id,omitempty"`
	Name            string `json:"name"`
	Phone           string `json:"phone"`
	IsComplete      bool   `json:"isComplete"`
	IsRegularClient bool   `json:"isRegularClient"`
}

type UpdateProfileRequest struct {
	Name  string `json:"name"`
	Phone string `json:"phone"`
}

type UpdateProfileResponse struct {
	ClientProfile
	SessionToken string `json:"sessionToken,omitempty"`
}

type EquipmentChoice struct {
	Mode          string `json:"mode"`
	RentalShoes   bool   `json:"rentalShoes"`
	RentalHarness bool   `json:"rentalHarness"`
}

type CreateBookingRequest struct {
	SlotID    string          `json:"slotId"`
	Client    ClientContacts  `json:"client"`
	Equipment EquipmentChoice `json:"equipment"`
}

type SlotRef struct {
	ID         string             `json:"id,omitempty"`
	StartsAt   time.Time          `json:"startsAt"`
	EndsAt     time.Time          `json:"endsAt,omitempty"`
	Format     TrainingFormatInfo `json:"format,omitempty"`
	Zone       ZoneInfo           `json:"zone,omitempty"`
	Instructor InstructorDetail   `json:"instructor,omitempty"`
}

type InstructorRating struct {
	ID        string    `json:"id"`
	Stars     int       `json:"stars"`
	CreatedAt time.Time `json:"createdAt"`
}

type Booking struct {
	ID                  string            `json:"id"`
	SlotID              string            `json:"slotId"`
	Status              string            `json:"status"`
	Equipment           EquipmentChoice   `json:"equipment"`
	TotalPrice          float64           `json:"totalPrice,omitempty"`
	PriceBreakdown      *PriceBreakdown   `json:"priceBreakdown,omitempty"`
	CancellationReason  *string           `json:"cancellationReason,omitempty"`
	CancelledAt         *time.Time        `json:"cancelledAt,omitempty"`
	WaitlistPosition    *int              `json:"waitlistPosition,omitempty"`
	CreatedAt           time.Time         `json:"createdAt"`
	Slot                *SlotRef          `json:"slot,omitempty"`
	Gym                 *GymInfo          `json:"gym,omitempty"`
	InstructorRating    *InstructorRating `json:"instructorRating"`
}

type CreateBookingResponse struct {
	Booking
	SessionToken string `json:"sessionToken,omitempty"`
}

type BookingSummary struct {
	ID               string  `json:"id"`
	Status           string  `json:"status"`
	TotalPrice       float64 `json:"totalPrice"`
	WaitlistPosition *int    `json:"waitlistPosition,omitempty"`
	Slot             struct {
		StartsAt   time.Time `json:"startsAt"`
		Format     struct {
			Name string `json:"name"`
		} `json:"format,omitempty"`
		Zone struct {
			Name string `json:"name"`
		} `json:"zone,omitempty"`
		Instructor struct {
			FullName string `json:"fullName"`
		} `json:"instructor,omitempty"`
	} `json:"slot"`
}

type BookingListResponse struct {
	Items []BookingSummary `json:"items"`
	Meta  PaginationMeta   `json:"meta,omitempty"`
}

type CancelBookingResponse struct {
	ID          string    `json:"id"`
	Status      string    `json:"status"`
	CancelledAt time.Time `json:"cancelledAt"`
}

type WaitlistEntry struct {
	ID        string       `json:"id"`
	SlotID    string       `json:"slotId"`
	Position  int          `json:"position"`
	Status    string       `json:"status"`
	CreatedAt time.Time    `json:"createdAt"`
	Slot      *SlotSummary `json:"slot,omitempty"`
}

type JoinWaitlistResponse struct {
	WaitlistEntry
	SessionToken string `json:"sessionToken,omitempty"`
}

type JoinWaitlistRequest struct {
	Client *ClientContacts `json:"client,omitempty"`
}

type CreateRatingRequest struct {
	BookingID    string `json:"bookingId"`
	InstructorID string `json:"instructorId"`
	Stars        int    `json:"stars"`
}

type CreateRatingResponse struct {
	ID           string    `json:"id"`
	BookingID    string    `json:"bookingId"`
	InstructorID string    `json:"instructorId"`
	Stars        int       `json:"stars"`
	CreatedAt    time.Time `json:"createdAt"`
}

type RegisterPushTokenRequest struct {
	Token    string `json:"token"`
	Platform string `json:"platform"`
}
